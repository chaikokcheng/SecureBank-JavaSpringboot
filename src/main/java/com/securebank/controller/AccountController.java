package com.securebank.controller;

import com.securebank.dto.BalanceResponse;
import com.securebank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for account endpoints.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Get account balance.
     * GET /api/accounts/balance
     * 
     * @param userDetails Authenticated user
     * @return Balance response
     */
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @AuthenticationPrincipal UserDetails userDetails) {

        BalanceResponse response = accountService.getBalance(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
