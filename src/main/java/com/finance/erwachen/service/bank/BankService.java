package com.finance.erwachen.service.bank;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface BankService {

    /**
     * Authenticate with the bank API
     * @param userId User identifier for the bank
     * @param credentials Authentication credentials
     * @return true if authentication is successful
     */
    boolean authenticate(String userId, String credentials);

    /**
     * Fetch all accounts for the authenticated user
     * @return List of accounts from the bank
     */
    List<Account> fetchAccounts();

    /**
     * Fetch transactions for a specific account within a date range
     * @param account The account to fetch transactions for
     * @param startDate Start date for transaction fetch
     * @param endDate End date for transaction fetch
     * @return List of transactions
     */
    List<Transaction> fetchTransactions(Account account, LocalDate startDate, LocalDate endDate);

    /**
     * Update account balance from bank
     * @param account The account to update
     * @return Updated account with current balance
     */
    Account updateAccountBalance(Account account);

    /**
     * Get the bank provider this service handles
     * @return The bank provider enum
     */
    Account.BankProvider getBankProvider();

    /**
     * Check if the service is properly configured and available
     * @return true if the service is available
     */
    boolean isAvailable();
}