package com.bankie.bankie_api.service;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public Page<Account> getAccountsForUser(Authentication auth, Pageable pageable) {
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (isEmployee) {
            return accountRepository.findAll(pageable);
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return accountRepository.findByUserId(user.getId(), pageable);
    }

    public Page<Account> searchAccounts(String firstName, String lastName, Pageable pageable, Authentication auth)
    {
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
        Page<Account> results = accountRepository.searchByOwnerNames(firstName, lastName, pageable);

        if (!isEmployee) {
            return accountRepository.searchApprovedByOwnerNames(firstName, lastName, pageable);
        }

        return results;
    }
}
