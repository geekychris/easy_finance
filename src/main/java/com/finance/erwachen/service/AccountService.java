package com.finance.erwachen.service;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.repository.AccountRepository;
import com.finance.erwachen.service.bank.BankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final Map<Account.BankProvider, BankService> bankServices;

    @Autowired
    public AccountService(AccountRepository accountRepository, List<BankService> bankServices) {
        this.accountRepository = accountRepository;
        this.bankServices = bankServices.stream()
                .collect(Collectors.toMap(BankService::getBankProvider, Function.identity()));
    }

    public List<Account> findAllActive() {
        return accountRepository.findAllActiveOrderByName();
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Account save(Account account) {
        logger.info("Saving account: {}", account.getAccountName());
        return accountRepository.save(account);
    }

    public void delete(Account account) {
        logger.info("Deactivating account: {}", account.getAccountName());
        account.setActive(false);
        accountRepository.save(account);
    }

    public void hardDelete(Account account) {
        logger.info("Hard deleting account: {}", account.getAccountName());
        accountRepository.delete(account);
    }

    public boolean hasTransactions(Account account) {
        return account.getTransactions() != null && !account.getTransactions().isEmpty();
    }

    public boolean accountNumberExists(String accountNumber) {
        return findByAccountNumber(accountNumber).isPresent();
    }

    public boolean accountNumberExistsExcludingId(String accountNumber, Long excludeId) {
        Optional<Account> account = findByAccountNumber(accountNumber);
        return account.isPresent() && !account.get().getId().equals(excludeId);
    }

    public List<Account> findByBankProvider(Account.BankProvider provider) {
        return accountRepository.findByBankProviderAndActiveTrue(provider);
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = accountRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalBalanceByAccountType(Account.AccountType accountType) {
        BigDecimal total = accountRepository.getTotalBalanceByAccountType(accountType);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Synchronize all accounts with their respective bank APIs
     */
    public void syncAllAccounts() {
        logger.info("Starting synchronization of all accounts");
        
        List<Account> accounts = findAllActive();
        
        for (Account account : accounts) {
            try {
                syncAccount(account);
            } catch (Exception e) {
                logger.error("Failed to sync account {}: {}", account.getAccountNumber(), e.getMessage(), e);
            }
        }
        
        logger.info("Completed synchronization of {} accounts", accounts.size());
    }

    /**
     * Synchronize a specific account with its bank API
     */
    public Account syncAccount(Account account) {
        logger.info("Syncing account: {}", account.getAccountNumber());
        
        BankService bankService = bankServices.get(account.getBankProvider());
        if (bankService == null) {
            logger.warn("No bank service available for provider: {}", account.getBankProvider());
            return account;
        }

        if (!bankService.isAvailable()) {
            logger.warn("Bank service for {} is not available", account.getBankProvider());
            return account;
        }

        try {
            Account updatedAccount = bankService.updateAccountBalance(account);
            updatedAccount.setLastSyncDate(LocalDateTime.now());
            return accountRepository.save(updatedAccount);
        } catch (Exception e) {
            logger.error("Failed to sync account {}: {}", account.getAccountNumber(), e.getMessage(), e);
            return account;
        }
    }

    /**
     * Fetch and save accounts from a specific bank provider
     */
    public List<Account> importAccountsFromBank(Account.BankProvider provider) {
        logger.info("Importing accounts from bank: {}", provider);
        
        BankService bankService = bankServices.get(provider);
        if (bankService == null) {
            logger.error("No bank service available for provider: {}", provider);
            return List.of();
        }

        if (!bankService.isAvailable()) {
            logger.error("Bank service for {} is not available", provider);
            return List.of();
        }

        try {
            List<Account> fetchedAccounts = bankService.fetchAccounts();
            List<Account> savedAccounts = fetchedAccounts.stream()
                    .map(this::saveOrUpdateAccount)
                    .collect(Collectors.toList());
            
            logger.info("Imported {} accounts from {}", savedAccounts.size(), provider);
            return savedAccounts;
        } catch (Exception e) {
            logger.error("Failed to import accounts from {}: {}", provider, e.getMessage(), e);
            return List.of();
        }
    }

    private Account saveOrUpdateAccount(Account account) {
        Optional<Account> existingAccount = findByAccountNumber(account.getAccountNumber());
        
        if (existingAccount.isPresent()) {
            Account existing = existingAccount.get();
            existing.setBalance(account.getBalance());
            existing.setLastSyncDate(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            return accountRepository.save(existing);
        } else {
            return accountRepository.save(account);
        }
    }

    /**
     * Get accounts that need synchronization (haven't been synced recently)
     */
    public List<Account> getAccountsNeedingSync(int hoursThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hoursThreshold);
        return accountRepository.findAccountsNeedingSync(threshold);
    }

    /**
     * Get summary statistics for all accounts
     */
    public AccountSummary getAccountSummary() {
        List<Account> accounts = findAllActive();
        
        BigDecimal totalAssets = accounts.stream()
                .filter(a -> a.getAccountType() != Account.AccountType.CREDIT_CARD && a.getAccountType() != Account.AccountType.LOAN)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalLiabilities = accounts.stream()
                .filter(a -> a.getAccountType() == Account.AccountType.CREDIT_CARD || a.getAccountType() == Account.AccountType.LOAN)
                .map(Account::getBalance)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);
        
        return new AccountSummary(accounts.size(), totalAssets, totalLiabilities, netWorth);
    }

    public static class AccountSummary {
        private final int totalAccounts;
        private final BigDecimal totalAssets;
        private final BigDecimal totalLiabilities;
        private final BigDecimal netWorth;

        public AccountSummary(int totalAccounts, BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal netWorth) {
            this.totalAccounts = totalAccounts;
            this.totalAssets = totalAssets;
            this.totalLiabilities = totalLiabilities;
            this.netWorth = netWorth;
        }

        public int getTotalAccounts() { return totalAccounts; }
        public BigDecimal getTotalAssets() { return totalAssets; }
        public BigDecimal getTotalLiabilities() { return totalLiabilities; }
        public BigDecimal getNetWorth() { return netWorth; }
    }
}