package com.finance.erwachen.repository;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Account.BankProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByActiveTrue();

    List<Account> findByBankProvider(BankProvider bankProvider);

    List<Account> findByBankProviderAndActiveTrue(BankProvider bankProvider);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.active = true ORDER BY a.accountName")
    List<Account> findAllActiveOrderByName();

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.active = true")
    BigDecimal getTotalBalance();

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.active = true AND a.accountType = :accountType")
    BigDecimal getTotalBalanceByAccountType(@Param("accountType") Account.AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.lastSyncDate < :syncDate AND a.active = true")
    List<Account> findAccountsNeedingSync(@Param("syncDate") LocalDateTime syncDate);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.bankProvider = :provider AND a.active = true")
    Long countActiveAccountsByProvider(@Param("provider") BankProvider provider);
}