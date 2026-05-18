package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.PageResponse;
import com.bankie.bankie_api.dto.request.SetAbsoluteLimitRequestDTO;
import com.bankie.bankie_api.dto.request.SetDailyLimitRequestDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.AccountMapper;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final UserMapper userMapper;

    @GetMapping
    public ResponseEntity<Page<AccountResponseDTO>> getAccounts(
            @PageableDefault(size = 20, sort = "iban") Pageable pageable,
            Authentication authentication) {

        Page<Account> accounts = accountService.getAccountsForUser(authentication, pageable);
        return ResponseEntity.ok(accounts.map(accountMapper::toResponseDto));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SearchAccountResponseDTO>> searchAccounts(
            @RequestParam(required = true) String firstName,
            @RequestParam(required = true) String lastName,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication){
        Page<Account> accounts = accountService.searchAccounts(firstName, lastName, pageable, authentication);
        System.out.println("Search Results Count: " + accounts.getTotalElements());
        accounts.getContent().forEach(a -> System.out.println("Found IBAN: " + a.getIban()));
        return ResponseEntity.ok(accounts.map(accountMapper::toSearchResponseDto));
    }

    @GetMapping("/customers/without-accounts")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<PageResponse<UserResponseDTO>> getCustomersWithoutAccounts(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = accountService.getCustomersWithoutAccounts(pageable);
        return ResponseEntity.ok(PageResponse.from(users.map(userMapper::toResponseDto)));
    }

    @GetMapping("/customers/{customerId}/accounts")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<AccountResponseDTO>> getAccountsByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Account> accounts = accountService.getAccountsByCustomer(customerId, pageable);
        return ResponseEntity.ok(accounts.map(accountMapper::toResponseDto));
    }

    @PatchMapping("/{iban}/close")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> closeAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountMapper.toResponseDto(accountService.closeAccount(iban)));
    }

    @PatchMapping("/{iban}/absolute-limit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> updateAbsoluteLimit(
            @PathVariable String iban,
            @RequestBody SetAbsoluteLimitRequestDTO dto) {
        return ResponseEntity.ok(accountMapper.toResponseDto(accountService.updateAbsoluteLimit(iban, dto)));
    }

    @PatchMapping("/{iban}/daily-limit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> updateDailyLimit(
            @PathVariable String iban,
            @RequestBody SetDailyLimitRequestDTO dto) {
        return ResponseEntity.ok(accountMapper.toResponseDto(accountService.updateDailyTransferLimit(iban, dto)));
    }
}