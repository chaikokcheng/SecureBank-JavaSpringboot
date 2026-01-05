package com.securebank.controller;

import com.securebank.dto.TransactionResponse;
import com.securebank.dto.TransferRequest;
import com.securebank.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for fund transfer endpoints.
 */
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Transfer funds to another account.
     * POST /api/transfer
     * 
     * @param userDetails Authenticated user
     * @param request     Transfer details
     * @return Transaction response
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {

        TransactionResponse response = transferService.transfer(
                userDetails.getUsername(),
                request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction history.
     * GET /api/transfer/history
     * 
     * @param userDetails Authenticated user
     * @return List of transactions
     */
    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<TransactionResponse> transactions = transferService.getTransactionHistory(
                userDetails.getUsername());
        return ResponseEntity.ok(transactions);
    }
}
