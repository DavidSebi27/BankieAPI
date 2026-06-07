package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.AuthContext;
import com.bankie.bankie_api.dto.request.AccountSearchFilterDTO;
import com.bankie.bankie_api.dto.request.CreateAccountRequestDTO;
import com.bankie.bankie_api.dto.request.SetAbsoluteLimitRequestDTO;
import com.bankie.bankie_api.dto.request.SetDailyLimitRequestDTO;
import com.bankie.bankie_api.dto.request.VerifyRecipientRequestDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<Page<AccountResponseDTO>> getAccounts(
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "iban") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccountsForUser(customerId, AuthContext.from(authentication), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SearchAccountResponseDTO>> searchAccounts(
            @Valid @ParameterObject AccountSearchFilterDTO filter,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.searchAccounts(filter, pageable, AuthContext.from(authentication)));
    }

    @GetMapping("/verify-recipient")
    public ResponseEntity<SearchAccountResponseDTO> verifyRecipient(
            @Valid @ParameterObject VerifyRecipientRequestDTO request) {
        return ResponseEntity.ok(accountService.verifyRecipient(request));
    }

    @PostMapping("/customers/{customerId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AccountResponseDTO> approveCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CreateAccountRequestDTO dto) {
        return accountService.approveCustomerAndCreateAccounts(customerId, dto);
    }

    @PatchMapping("/{iban}/close")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> closeAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.closeAccount(iban));
    }

    @PatchMapping("/{iban}/absolute-limit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> updateAbsoluteLimit(
            @PathVariable String iban,
            @Valid @RequestBody SetAbsoluteLimitRequestDTO dto) {
        return ResponseEntity.ok(accountService.updateAbsoluteLimit(iban, dto));
    }

    @PatchMapping("/{iban}/daily-limit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> updateDailyLimit(
            @PathVariable String iban,
            @Valid @RequestBody SetDailyLimitRequestDTO dto) {
        return ResponseEntity.ok(accountService.updateDailyTransferLimit(iban, dto));
    }
}