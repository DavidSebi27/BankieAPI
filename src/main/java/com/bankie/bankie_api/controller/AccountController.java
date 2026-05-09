package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
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

}