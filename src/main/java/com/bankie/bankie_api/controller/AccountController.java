package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.AuthContext;
import com.bankie.bankie_api.dto.request.AccountSearchFilterDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<Page<AccountResponseDTO>> getAccounts(
            @PageableDefault(size = 20, sort = "iban") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccountsForUser(AuthContext.from(authentication), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SearchAccountResponseDTO>> searchAccounts(
            @Valid @ParameterObject AccountSearchFilterDTO filter,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(accountService.searchAccounts(filter, pageable, AuthContext.from(authentication)));
    }
}
