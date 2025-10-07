package com.finance.erwachen.config;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Category;
import com.finance.erwachen.entity.Transaction;
import com.finance.erwachen.service.AccountService;
import com.finance.erwachen.service.CategoryService;
import com.finance.erwachen.service.TransactionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final Random random = new Random();

    @Autowired
    public DataInitializer(AccountService accountService, CategoryService categoryService, 
                          TransactionService transactionService) {
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    @PostConstruct
    public void initializeData() {
        logger.info("Initializing sample data...");
        
        try {
            // Create default categories first
            categoryService.createDefaultCategories();
            
            // Import sample accounts from both banks
            List<Account> schwabAccounts = accountService.importAccountsFromBank(Account.BankProvider.SCHWAB);
            List<Account> bofaAccounts = accountService.importAccountsFromBank(Account.BankProvider.BANK_OF_AMERICA);
            
            // Create sample transactions
            createSampleTransactions(schwabAccounts);
            createSampleTransactions(bofaAccounts);
            
            logger.info("Sample data initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize sample data", e);
        }
    }

    private void createSampleTransactions(List<Account> accounts) {
        if (accounts.isEmpty()) {
            return;
        }
        
        List<Category> categories = categoryService.findAllActive();
        
        for (Account account : accounts) {
            logger.info("Creating sample transactions for account: {}", account.getAccountName());
            
            // Import transactions for the last 3 months
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();
            
            try {
                List<Transaction> transactions = transactionService
                    .importTransactionsFromBank(account, startDate, endDate);
                
                // Auto-categorize some of the transactions
                transactions.stream()
                    .filter(t -> t.getCategory() == null)
                    .limit(transactions.size() / 2) // Categorize half of them
                    .forEach(transaction -> {
                        Transaction categorized = transactionService
                            .categorizeTransaction(transaction, categories);
                        
                        // If automatic categorization didn't work, assign a random category
                        if (categorized.getCategory() == null && !categories.isEmpty()) {
                            Category randomCategory = categories.get(random.nextInt(categories.size()));
                            categorized.setCategory(randomCategory);
                        }
                        
                        transactionService.save(categorized);
                    });
                
                logger.info("Created {} sample transactions for account {}", 
                           transactions.size(), account.getAccountName());
            } catch (Exception e) {
                logger.error("Failed to create sample transactions for account {}: {}", 
                            account.getAccountName(), e.getMessage());
            }
        }
    }
}