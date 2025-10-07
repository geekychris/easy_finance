package com.finance.erwachen.repository;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Category;
import com.finance.erwachen.entity.Transaction;
import com.finance.erwachen.entity.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountOrderByTransactionDateDesc(Account account, Pageable pageable);

    List<Transaction> findByAccountAndTransactionDateBetween(Account account, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    Page<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Transaction> findByCategory(Category category);

    Page<Transaction> findByCategoryOrderByTransactionDateDesc(Category category, Pageable pageable);

    Optional<Transaction> findByBankTransactionId(String bankTransactionId);

    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.reconciled = false ORDER BY t.transactionDate DESC")
    List<Transaction> findUnreconciledByAccount(@Param("account") Account account);

    @Query("SELECT t FROM Transaction t WHERE t.description ILIKE %:searchTerm% OR t.payee ILIKE %:searchTerm% OR t.memo ILIKE %:searchTerm%")
    Page<Transaction> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account = :account AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getSumByAccountAndDateRange(@Param("account") Account account, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.category = :category AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getSumByCategoryAndDateRange(@Param("category") Category category, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getSumByTypeAndDateRange(@Param("type") TransactionType type, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :date")
    Long countTransactionsSince(@Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t WHERE t.amount < 0 AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.amount ASC")
    List<Transaction> findExpenseTransactionsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.amount > 0 AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.amount DESC")
    List<Transaction> findIncomeTransactionsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}