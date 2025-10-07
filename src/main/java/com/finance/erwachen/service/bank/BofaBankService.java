package com.finance.erwachen.service.bank;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class BofaBankService implements BankService {

    private static final Logger logger = LoggerFactory.getLogger(BofaBankService.class);

    @Value("${bank.bofa.api-url}")
    private String apiUrl;

    @Value("${bank.bofa.client-id}")
    private String clientId;

    @Value("${bank.bofa.client-secret}")
    private String clientSecret;

    private final WebClient webClient;
    private final Random random = new Random();

    public BofaBankService() {
        this.webClient = WebClient.builder().build();
    }

    @Override
    public boolean authenticate(String userId, String credentials) {
        logger.info("Attempting to authenticate with Bank of America for user: {}", userId);
        
        // TODO: Implement actual Bank of America API authentication
        // This is a placeholder implementation
        try {
            // Simulate API call delay
            Thread.sleep(1200);
            
            // For now, return true if credentials are provided
            boolean authenticated = credentials != null && !credentials.trim().isEmpty();
            logger.info("Bank of America authentication result: {}", authenticated);
            return authenticated;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Authentication interrupted", e);
            return false;
        } catch (Exception e) {
            logger.error("Error during Bank of America authentication", e);
            return false;
        }
    }

    @Override
    public List<Account> fetchAccounts() {
        logger.info("Fetching accounts from Bank of America");
        
        // TODO: Implement actual Bank of America API integration
        // This is a placeholder that returns sample data
        List<Account> accounts = new ArrayList<>();
        
        // Create sample Bank of America accounts
        Account checkingAccount = new Account(
            "BOFA-CHK-001",
            "Bank of America Core Checking",
            Account.AccountType.CHECKING,
            Account.BankProvider.BANK_OF_AMERICA
        );
        checkingAccount.setBalance(new BigDecimal("2847.33"));
        checkingAccount.setDescription("Primary checking account");
        accounts.add(checkingAccount);
        
        Account savingsAccount = new Account(
            "BOFA-SAV-001", 
            "Bank of America Advantage Savings",
            Account.AccountType.SAVINGS,
            Account.BankProvider.BANK_OF_AMERICA
        );
        savingsAccount.setBalance(new BigDecimal("15620.89"));
        savingsAccount.setDescription("High yield savings account");
        accounts.add(savingsAccount);
        
        Account creditCardAccount = new Account(
            "BOFA-CC-001", 
            "Bank of America Cash Rewards Credit Card",
            Account.AccountType.CREDIT_CARD,
            Account.BankProvider.BANK_OF_AMERICA
        );
        creditCardAccount.setBalance(new BigDecimal("-1245.67")); // Negative balance for credit card
        creditCardAccount.setDescription("Cash back credit card");
        accounts.add(creditCardAccount);
        
        logger.info("Retrieved {} accounts from Bank of America", accounts.size());
        return accounts;
    }

    @Override
    public List<Transaction> fetchTransactions(Account account, LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching transactions for account {} from {} to {}", 
                   account.getAccountNumber(), startDate, endDate);
        
        // TODO: Implement actual Bank of America API integration
        // This is a placeholder that returns sample transactions
        List<Transaction> transactions = new ArrayList<>();
        
        // Generate sample transactions based on account type
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (random.nextDouble() > 0.6) { // 40% chance of transaction per day
                Transaction transaction = createSampleTransaction(account, currentDate);
                transactions.add(transaction);
            }
            currentDate = currentDate.plusDays(1);
        }
        
        logger.info("Retrieved {} transactions for account {}", transactions.size(), account.getAccountNumber());
        return transactions;
    }

    @Override
    public Account updateAccountBalance(Account account) {
        logger.info("Updating balance for account: {}", account.getAccountNumber());
        
        // TODO: Implement actual balance fetch from Bank of America API
        // This is a placeholder that simulates balance updates
        BigDecimal currentBalance = account.getBalance();
        BigDecimal variation = new BigDecimal(random.nextDouble() * 80 - 40); // Random change between -40 and +40
        BigDecimal newBalance = currentBalance.add(variation);
        
        account.setBalance(newBalance);
        account.setLastSyncDate(LocalDateTime.now());
        
        logger.info("Updated balance for account {} to {}", account.getAccountNumber(), newBalance);
        return account;
    }

    @Override
    public Account.BankProvider getBankProvider() {
        return Account.BankProvider.BANK_OF_AMERICA;
    }

    @Override
    public boolean isAvailable() {
        // TODO: Implement actual health check for Bank of America API
        // For now, check if configuration is present
        boolean available = apiUrl != null && !apiUrl.trim().isEmpty() && 
                          clientId != null && !clientId.trim().isEmpty();
        logger.debug("Bank of America service availability: {}", available);
        return available;
    }

    private Transaction createSampleTransaction(Account account, LocalDate date) {
        String[] checkingDescriptions = {
            "Walmart Supercenter", "Shell Gas Station", "Starbucks Coffee", 
            "Amazon.com Purchase", "ATM Withdrawal", "Payroll Deposit", 
            "Electric Bill Payment", "Verizon Wireless", "Netflix Subscription"
        };
        
        String[] savingsDescriptions = {
            "Interest Payment", "Transfer from Checking", "Monthly Savings Goal",
            "Transfer to Checking", "Dividend Payment"
        };
        
        String[] creditCardDescriptions = {
            "Target Store", "Uber Ride", "DoorDash Order", "Best Buy", 
            "Payment Received", "Interest Charge", "Late Fee", "Cash Advance"
        };
        
        String[] descriptions = checkingDescriptions;
        BigDecimal baseAmount = new BigDecimal(random.nextDouble() * 150 - 75);
        
        if (account.getAccountType() == Account.AccountType.SAVINGS) {
            descriptions = savingsDescriptions;
            baseAmount = new BigDecimal(random.nextDouble() * 500 - 250);
        } else if (account.getAccountType() == Account.AccountType.CREDIT_CARD) {
            descriptions = creditCardDescriptions;
            baseAmount = new BigDecimal(random.nextDouble() * 300 - 150);
            // For credit cards, reverse the sign (positive = charge, negative = payment)
            baseAmount = baseAmount.negate();
        }
        
        String description = descriptions[random.nextInt(descriptions.length)];
        Transaction.TransactionType type = baseAmount.compareTo(BigDecimal.ZERO) > 0 ? 
            Transaction.TransactionType.CREDIT : Transaction.TransactionType.DEBIT;
        
        Transaction transaction = new Transaction(account, description, baseAmount, date, type);
        transaction.setBankTransactionId("BOFA-TXN-" + System.currentTimeMillis() + "-" + random.nextInt(1000));
        transaction.setPostedDate(date);
        
        return transaction;
    }
}