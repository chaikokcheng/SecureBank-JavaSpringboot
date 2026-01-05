package com.securebank.repository;

import com.securebank.model.Account;
import com.securebank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Transaction entity operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySenderAccountOrderByTimestampDesc(Account senderAccount);

    List<Transaction> findByReceiverAccountOrderByTimestampDesc(Account receiverAccount);

    /**
     * Find all transactions where the account is either sender or receiver.
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderAccount = :account OR t.receiverAccount = :account ORDER BY t.timestamp DESC")
    List<Transaction> findAllByAccount(@Param("account") Account account);

    /**
     * Find transactions by account ID (sender or receiver).
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderAccount.id = :accountId OR t.receiverAccount.id = :accountId ORDER BY t.timestamp DESC")
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);
}
