package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.CreateAccountRequestDTO;
import com.bankie.bankie_api.dto.request.UpdateLimitsRequestDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.util.IbanGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final IbanGenerator ibanGenerator;

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

    public Page<Account> searchAccounts(String firstName, String lastName, Pageable pageable, Authentication auth) {
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
        Page<Account> results = accountRepository.searchByOwnerNames(firstName, lastName, pageable);

        if (!isEmployee) {
            return accountRepository.searchApprovedByOwnerNames(firstName, lastName, pageable);
        }

        return results;
    }

    public Page<User> getCustomersWithoutAccounts(Pageable pageable) {
        return userRepository.findByRoleAndNoAccounts(Role.CUSTOMER, pageable);
    }

    @Transactional
    public List<Account> approveCustomerAndCreateAccounts(Long userId, CreateAccountRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomerNotFoundException(userId));

        if (!accountRepository.findByUser(user).isEmpty()) {
            throw new BusinessRuleException("Customer already has accounts");
        }

        user.setApproved(true);
        userRepository.save(user);

        Account checking = Account.builder()
                .iban(ibanGenerator.generate())
                .type(AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .absoluteLimit(dto.getAbsoluteLimit() != null ? dto.getAbsoluteLimit() : BigDecimal.ZERO)
                .dailyTransferLimit(dto.getDailyTransferLimit() != null ? dto.getDailyTransferLimit() : new BigDecimal("1000.00"))
                .user(user)
                .build();

        Account savings = Account.builder()
                .iban(ibanGenerator.generate())
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .absoluteLimit(BigDecimal.ZERO)
                .dailyTransferLimit(dto.getDailyTransferLimit() != null ? dto.getDailyTransferLimit() : new BigDecimal("1000.00"))
                .user(user)
                .build();

        accountRepository.save(checking);
        accountRepository.save(savings);

        return List.of(checking, savings);
    }

    @Transactional
    public Account closeAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + iban));
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Account is already closed");
        }
        account.setStatus(AccountStatus.CLOSED);
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAbsoluteLimit(String iban, UpdateLimitsRequestDTO dto) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + iban));
        if (dto.getAbsoluteLimit() == null) {
            throw new BusinessRuleException("absoluteLimit is required");
        }
        account.setAbsoluteLimit(dto.getAbsoluteLimit());
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateDailyTransferLimit(String iban, UpdateLimitsRequestDTO dto) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + iban));
        if (dto.getDailyTransferLimit() == null) {
            throw new BusinessRuleException("dailyTransferLimit is required");
        }
        if (dto.getDailyTransferLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("dailyTransferLimit cannot be negative");
        }
        account.setDailyTransferLimit(dto.getDailyTransferLimit());
        return accountRepository.save(account);
    }
}