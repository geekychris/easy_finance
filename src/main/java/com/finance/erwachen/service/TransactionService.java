package com.finance.erwachen.service;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Category;
import com.finance.erwachen.entity.Transaction;
import com.finance.erwachen.repository.TransactionRepository;
import com.finance.erwachen.service.bank.BankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final Map<Account.BankProvider, BankService> bankServices;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, List<BankService> bankServices) {
        this.transactionRepository = transactionRepository;
        this.bankServices = bankServices.stream()
                .collect(Collectors.toMap(BankService::getBankProvider, Function.identity()));
    }

    public Page<Transaction> findAll(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    public Page<Transaction> findByAccount(Account account, Pageable pageable) {
        return transactionRepository.findByAccountOrderByTransactionDateDesc(account, pageable);
    }

    public Page<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate, pageable);
    }

    public Page<Transaction> findByCategory(Category category, Pageable pageable) {
        return transactionRepository.findByCategoryOrderByTransactionDateDesc(category, pageable);
    }
    
    public List<Transaction> getRecentTransactions(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "transactionDate", "id"));
        Page<Transaction> page = transactionRepository.findAll(pageRequest);
        return page.getContent();
    }

    public Page<Transaction> searchTransactions(String searchTerm, Pageable pageable) {
        return transactionRepository.findBySearchTerm(searchTerm, pageable);
    }

    public Transaction save(Transaction transaction) {
        logger.info("Saving transaction: {} for amount: {}", transaction.getDescription(), transaction.getAmount());
        return transactionRepository.save(transaction);
    }

    public void delete(Transaction transaction) {
        logger.info("Deleting transaction: {}", transaction.getId());
        transactionRepository.delete(transaction);
    }

    public List<Transaction> findUnreconciledByAccount(Account account) {
        return transactionRepository.findUnreconciledByAccount(account);
    }

    /**
     * Import transactions from bank for a specific account and date range
     */
    public List<Transaction> importTransactionsFromBank(Account account, LocalDate startDate, LocalDate endDate) {
        logger.info("Importing transactions for account {} from {} to {}", 
                   account.getAccountNumber(), startDate, endDate);
        
        BankService bankService = bankServices.get(account.getBankProvider());
        if (bankService == null) {
            logger.error("No bank service available for provider: {}", account.getBankProvider());
            return List.of();
        }

        if (!bankService.isAvailable()) {
            logger.error("Bank service for {} is not available", account.getBankProvider());
            return List.of();
        }

        try {
            List<Transaction> fetchedTransactions = bankService.fetchTransactions(account, startDate, endDate);
            List<Transaction> savedTransactions = fetchedTransactions.stream()
                    .filter(this::isNewTransaction)
                    .map(this::save)
                    .collect(Collectors.toList());
            
            logger.info("Imported {} new transactions for account {}", 
                       savedTransactions.size(), account.getAccountNumber());
            return savedTransactions;
        } catch (Exception e) {
            logger.error("Failed to import transactions for account {}: {}", 
                        account.getAccountNumber(), e.getMessage(), e);
            return List.of();
        }
    }

    private boolean isNewTransaction(Transaction transaction) {
        if (transaction.getBankTransactionId() != null) {
            return transactionRepository.findByBankTransactionId(transaction.getBankTransactionId()).isEmpty();
        }
        
        // If no bank transaction ID, check for duplicates based on account, amount, date, and description
        List<Transaction> existing = transactionRepository.findByAccountAndTransactionDateBetween(
                transaction.getAccount(), 
                transaction.getTransactionDate(), 
                transaction.getTransactionDate()
        );
        
        return existing.stream().noneMatch(t -> 
                t.getAmount().equals(transaction.getAmount()) && 
                t.getDescription().equals(transaction.getDescription())
        );
    }

    /**
     * Get expense summary for a date range
     */
    public ExpenseSummary getExpenseSummary(LocalDate startDate, LocalDate endDate) {
        List<Transaction> expenseTransactions = transactionRepository.findExpenseTransactionsBetweenDates(startDate, endDate);
        List<Transaction> incomeTransactions = transactionRepository.findIncomeTransactionsBetweenDates(startDate, endDate);
        
        BigDecimal totalExpenses = expenseTransactions.stream()
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalIncome = incomeTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netIncome = totalIncome.subtract(totalExpenses);
        
        return new ExpenseSummary(
                totalIncome, 
                totalExpenses, 
                netIncome,
                incomeTransactions.size(),
                expenseTransactions.size()
        );
    }

    /**
     * Get spending by category for a date range
     */
    public Map<Category, BigDecimal> getSpendingByCategory(LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(startDate, endDate);
        
        return transactions.stream()
                .filter(t -> t.getCategory() != null && t.isExpense())
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.mapping(
                                t -> t.getAmount().abs(),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
    }

    /**
     * Reconcile transactions by marking them as reconciled
     */
    public void reconcileTransactions(List<Transaction> transactions) {
        logger.info("Reconciling {} transactions", transactions.size());
        
        transactions.forEach(transaction -> {
            transaction.setReconciled(true);
            transaction.setStatus(Transaction.TransactionStatus.RECONCILED);
        });
        
        transactionRepository.saveAll(transactions);
        logger.info("Successfully reconciled {} transactions", transactions.size());
    }

    /**
     * Categorize transaction automatically based on description and payee
     */
    public Transaction categorizeTransaction(Transaction transaction, List<Category> availableCategories) {
        if (transaction.getCategory() != null) {
            return transaction; // Already categorized
        }
        
        String description = transaction.getDescription().toLowerCase();
        String payee = transaction.getPayee() != null ? transaction.getPayee().toLowerCase() : "";
        
        // Simple categorization logic - can be enhanced with machine learning
        Optional<Category> matchedCategory = availableCategories.stream()
                .filter(category -> {
                    String categoryName = category.getName().toLowerCase();
                    return description.contains(categoryName) || payee.contains(categoryName) ||
                           matchesCommonPatterns(description, payee, category);
                })
                .findFirst();
        
        matchedCategory.ifPresent(transaction::setCategory);
        return transaction;
    }

    private boolean matchesCommonPatterns(String description, String payee, Category category) {
        String categoryName = category.getName().toLowerCase();
        
        // Common patterns for different categories
        switch (categoryName) {
            case "groceries":
            case "food":
                return description.contains("grocery") || description.contains("supermarket") ||
                       description.contains("walmart") || description.contains("target") ||
                       payee.contains("market");
            case "gas":
            case "fuel":
                return description.contains("gas") || description.contains("fuel") ||
                       description.contains("shell") || description.contains("exxon") ||
                       payee.contains("gas");
            case "dining":
            case "restaurant":
                return description.contains("restaurant") || description.contains("cafe") ||
                       description.contains("mcdonald") || description.contains("starbucks") ||
                       payee.contains("restaurant");
            case "utilities":
                return description.contains("electric") || description.contains("water") ||
                       description.contains("internet") || description.contains("phone") ||
                       payee.contains("utility");
            default:
                return false;
        }
    }

    public static class ExpenseSummary {
        private final BigDecimal totalIncome;
        private final BigDecimal totalExpenses;
        private final BigDecimal netIncome;
        private final int incomeTransactionCount;
        private final int expenseTransactionCount;

        public ExpenseSummary(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netIncome, 
                             int incomeTransactionCount, int expenseTransactionCount) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netIncome = netIncome;
            this.incomeTransactionCount = incomeTransactionCount;
            this.expenseTransactionCount = expenseTransactionCount;
        }

        public BigDecimal getTotalIncome() { return totalIncome; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public BigDecimal getNetIncome() { return netIncome; }
        public int getIncomeTransactionCount() { return incomeTransactionCount; }
        public int getExpenseTransactionCount() { return expenseTransactionCount; }
    }
}