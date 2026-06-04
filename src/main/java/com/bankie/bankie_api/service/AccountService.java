package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.AuthContext;
import com.bankie.bankie_api.dto.request.AccountSearchFilterDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.mapper.AccountMapper;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    public Page<AccountResponseDTO> getAccountsForUser(AuthContext authContext, Pageable pageable) {
        Page<Account> accounts = authContext.isEmployee()
                ? accountRepository.findAll(pageable)
                : accountRepository.findByUserId(currentUserId(authContext.email()), pageable);
        return accounts.map(accountMapper::toResponseDto);
    }

    public Page<SearchAccountResponseDTO> searchAccounts(AccountSearchFilterDTO filter, Pageable pageable,
                                                         AuthContext authContext) {
        Page<Account> results = authContext.isEmployee()
                ? accountRepository.searchByOwnerNames(filter.firstName(), filter.lastName(), pageable)
                : accountRepository.searchApprovedByOwnerNames(filter.firstName(), filter.lastName(), pageable);
        return results.map(accountMapper::toSearchResponseDto);
    }

    private Long currentUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();
    }
}
