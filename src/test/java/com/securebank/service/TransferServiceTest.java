package com.securebank.service;

import com.securebank.dto.TransferRequest;
import com.securebank.dto.TransactionResponse;
import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.ResourceNotFoundException;
import com.securebank.model.*;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransferService.
 * Tests the core business logic for fund transfers.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    private User sender;
    private User receiver;
    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .email("sender@test.com")
                .password("encoded")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .build();

        receiver = User.builder()
                .id(2L)
                .email("receiver@test.com")
                .password("encoded")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.USER)
                .build();

        senderAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC001")
                .balance(new BigDecimal("1000.00"))
                .user(sender)
                .build();

        receiverAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC002")
                .balance(new BigDecimal("500.00"))
                .user(receiver)
                .build();
    }

    @Test
    @DisplayName("Transfer should succeed when sender has sufficient funds")
    void transfer_Success() {
        // Arrange
        TransferRequest request = new TransferRequest("ACC002", new BigDecimal("100.00"), "Test transfer");

        when(userRepository.findByEmail("sender@test.com")).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumberWithLock("ACC002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(1L);
            return tx;
        });

        // Act
        TransactionResponse response = transferService.transfer("sender@test.com", request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getSenderAccountNumber()).isEqualTo("ACC001");
        assertThat(response.getReceiverAccountNumber()).isEqualTo("ACC002");

        // Verify balances were updated
        assertThat(senderAccount.getBalance()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(receiverAccount.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));

        // Verify save was called for both accounts
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Transfer should fail with InsufficientFundsException when balance is too low")
    void transfer_InsufficientFunds_ThrowsException() {
        // Arrange
        TransferRequest request = new TransferRequest("ACC002", new BigDecimal("2000.00"), "Too much");

        when(userRepository.findByEmail("sender@test.com")).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumberWithLock("ACC002")).thenReturn(Optional.of(receiverAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer("sender@test.com", request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        // Verify balances remain unchanged
        assertThat(senderAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(receiverAccount.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Transfer should fail when sender account not found")
    void transfer_SenderNotFound_ThrowsException() {
        // Arrange
        TransferRequest request = new TransferRequest("ACC002", new BigDecimal("100.00"), "Test");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer("unknown@test.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Transfer should fail when receiver account not found")
    void transfer_ReceiverNotFound_ThrowsException() {
        // Arrange
        TransferRequest request = new TransferRequest("INVALID", new BigDecimal("100.00"), "Test");

        when(userRepository.findByEmail("sender@test.com")).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumberWithLock("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer("sender@test.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Transfer to self should fail")
    void transfer_ToSelf_ThrowsException() {
        // Arrange
        TransferRequest request = new TransferRequest("ACC001", new BigDecimal("100.00"), "Self transfer");

        when(userRepository.findByEmail("sender@test.com")).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(senderAccount));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer("sender@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer to your own account");
    }
}
