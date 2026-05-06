package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.response.AccountResponse;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @GetMapping("/accounts")
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return accountRepository.findByOwner(user).stream().map(AccountResponse::from).toList();
    }
}