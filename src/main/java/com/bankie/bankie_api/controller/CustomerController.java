package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.PageResponse;
import com.bankie.bankie_api.dto.request.CreateAccountRequestDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.AccountMapper;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.service.AccountService;
import com.bankie.bankie_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<PageResponse<UserResponseDTO>> getCustomers(
            @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        Page<User> users = switch (status == null ? "all" : status) {
            case "no-accounts" -> accountService.getCustomersWithoutAccounts(pageable);
            case "all-closed"  -> accountService.getCustomersWithAllAccountsClosed(pageable);
            case "all"         -> accountService.getAllCustomers(pageable);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown status filter: " + status + ". Valid values: no-accounts, all-closed"
            );
        };

        return ResponseEntity.ok(PageResponse.from(users.map(userMapper::toResponseDto)));
    }

    @GetMapping("/{customerId}/transactions")
    public Page<TransactionResponseDTO> transactions(
            @PathVariable Long customerId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.findByCustomer(customerId, pageable);
    }

    @GetMapping("/{customerId}/accounts")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<AccountResponseDTO>> getAccountsByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Account> accounts = accountService.getAccountsByCustomer(customerId, pageable);
        return ResponseEntity.ok(accounts.map(accountMapper::toResponseDto));
    }

    @PostMapping("/{customerId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<AccountResponseDTO>> approveCustomer(
            @PathVariable Long customerId,
            @RequestBody CreateAccountRequestDTO dto) {
        List<Account> accounts = accountService.approveCustomerAndCreateAccounts(customerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accounts.stream().map(accountMapper::toResponseDto).toList());
    }
}