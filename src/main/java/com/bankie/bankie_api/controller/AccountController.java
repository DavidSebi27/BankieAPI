package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.AccountResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.mapper.AccountMapper;
import com.bankie.bankie_api.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @GetMapping
    public ResponseEntity<Page<AccountResponseDTO>> getAccounts(
            @PageableDefault(size = 20, sort = "iban") Pageable pageable,
            Authentication authentication) {

        Page<Account> accounts = accountService.getAccountsForUser(authentication, pageable);
        return ResponseEntity.ok(accounts.map(accountMapper::toResponseDto));
    }

}