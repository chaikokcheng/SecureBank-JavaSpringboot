package com.securebank.service;

import com.securebank.dto.TransactionResponse;
import com.securebank.dto.TransferRequest;
import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.ResourceNotFoundException;
import com.securebank.model.Account;
import com.securebank.model.Transaction;
import com.securebank.model.TransactionStatus;
import com.securebank.model.User;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling fund transfers with ACID-compliant transactions.
 * 
 * The @Transactional annotation ensures:
 * - Atomicity: All operations complete or none do
 * - Consistency: Database remains in valid state
 * - Isolation: Concurrent transactions don't interfere
 * - Durability: Committed changes persist
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Transfer funds from sender to receiver.
     * Uses pessimistic locking to prevent race conditions.
     * 
     * @throws InsufficientFundsException if sender has insufficient balance
     * @throws ResourceNotFoundException  if accounts not found
     */
    @Transactional
    public TransactionResponse transfer(String senderEmail, TransferRequest request) {
        log.info("Processing transfer from {} to {} for amount {}",
                senderEmail, request.getToAccountNumber(), request.getAmount());

        // Get sender's user and account
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", senderEmail));

        Account senderAccount = accountRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "userId", sender.getId()));

        // Prevent self-transfer
        if (senderAccount.getAccountNumber().equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to your own account");
        }

        // Get receiver account with lock for safe concurrent access
        Account receiverAccount = accountRepository.findByAccountNumberWithLock(request.getToAccountNumber())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Account", "accountNumber", request.getToAccountNumber()));

        BigDecimal amount = request.getAmount();

        // Validate sufficient funds
        if (senderAccount.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient funds for transfer. Available: {}, Requested: {}",
                    senderAccount.getBalance(), amount);

            // Record failed transaction
            Transaction failedTransaction = Transaction.builder()
                    .senderAccount(senderAccount)
                    .receiverAccount(receiverAccount)
                    .amount(amount)
                    .status(TransactionStatus.FAILED)
                    .description(request.getDescription())
                    .build();
            transactionRepository.save(failedTransaction);

            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Available: $%.2f, Requested: $%.2f",
                            senderAccount.getBalance(), amount));
        }

        // Perform the transfer
        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        // Save updated accounts
        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);

        // Record successful transaction
        Transaction transaction = Transaction.builder()
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Transfer successful. Transaction ID: {}", transaction.getId());

        return TransactionResponse.builder()
                .id(transaction.getId())
                .senderAccountNumber(senderAccount.getAccountNumber())
                .receiverAccountNumber(receiverAccount.getAccountNumber())
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .description(transaction.getDescription())
                .timestamp(transaction.getTimestamp())
                .type("SENT")
                .build();
    }

    /**
     * Get transaction history for a user's account.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "userId", user.getId()));

        List<Transaction> transactions = transactionRepository.findAllByAccount(account);

        return transactions.stream()
                .map(tx -> mapToResponse(tx, account))
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction tx, Account userAccount) {
        String type = tx.getSenderAccount().getId().equals(userAccount.getId()) ? "SENT" : "RECEIVED";

        return TransactionResponse.builder()
                .id(tx.getId())
                .senderAccountNumber(tx.getSenderAccount().getAccountNumber())
                .receiverAccountNumber(tx.getReceiverAccount().getAccountNumber())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .timestamp(tx.getTimestamp())
                .type(type)
                .build();
    }
}
