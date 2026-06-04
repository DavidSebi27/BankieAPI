package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.CreateAccountRequestDTO;
import com.bankie.bankie_api.dto.request.SetAbsoluteLimitRequestDTO;
import com.bankie.bankie_api.dto.request.SetDailyLimitRequestDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.AccountNotFoundException;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.util.IbanGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${bankie.account.default-daily-limit}")
    private BigDecimal defaultDailyLimit;

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

    @Transactional(readOnly = true)
    public Account verifyRecipient(String iban, String firstName, String lastName) {
        String normalizedIban = iban == null ? "" : iban.replaceAll("\\s", "").toUpperCase();
        String inFirst = firstName == null ? "" : firstName.trim();
        String inLast  = lastName  == null ? "" : lastName.trim();

        return accountRepository.findById(normalizedIban)
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .filter(a -> a.getUser() != null
                        && a.getUser().getRole() == Role.CUSTOMER
                        && a.getUser().isApproved())
                .filter(a -> equalsTrimCi(a.getUser().getFirstName(), inFirst)
                        && equalsTrimCi(a.getUser().getLastName(), inLast))
                .orElseThrow(() -> new AccountNotFoundException(normalizedIban));
    }

    private static boolean equalsTrimCi(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    public Page<User> getCustomersWithoutAccounts(Pageable pageable) {
        return userRepository.findByRoleAndNoAccounts(Role.CUSTOMER, pageable);
    }

    public Page<User> getCustomersWithAllAccountsClosed(Pageable pageable) {
        return userRepository.findByRoleAndAllAccountsClosed(Role.CUSTOMER, pageable);
    }

    @Transactional
    public List<Account> approveCustomerAndCreateAccounts(Long userId, CreateAccountRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomerNotFoundException(userId));

        if (user.getRole() != Role.CUSTOMER) {
            throw new BusinessRuleException("Only customers can be approved");
        }

        if (accountRepository.existsByUser(user)) {
            throw new BusinessRuleException("Customer already has accounts");
        }

        if (dto.getAbsoluteLimit() != null && dto.getAbsoluteLimit().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Absolute limit cannot be positive");
        }
        if (dto.getDailyTransferLimit() != null && dto.getDailyTransferLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Daily transfer limit cannot be negative");
        }

        user.setApproved(true);
        userRepository.save(user);

        BigDecimal absoluteLimit = dto.getAbsoluteLimit() != null ? dto.getAbsoluteLimit() : BigDecimal.ZERO;
        BigDecimal dailyLimit = dto.getDailyTransferLimit() != null ? dto.getDailyTransferLimit() : defaultDailyLimit;

        Account checking = Account.builder()
                .iban(ibanGenerator.generate())
                .type(AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .absoluteLimit(absoluteLimit)
                .dailyTransferLimit(dailyLimit)
                .user(user)
                .build();

        Account savings = Account.builder()
                .iban(ibanGenerator.generate())
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .absoluteLimit(BigDecimal.ZERO)
                .dailyTransferLimit(dailyLimit)
                .user(user)
                .build();

        accountRepository.save(checking);
        accountRepository.save(savings);

        return List.of(checking, savings);
    }

    @Transactional
    public Account closeAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Account is already closed");
        }
        account.setStatus(AccountStatus.CLOSED);
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAbsoluteLimit(String iban, SetAbsoluteLimitRequestDTO dto) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        if (dto.getAbsoluteLimit() == null) {
            throw new BusinessRuleException("absoluteLimit is required");
        }
        if (dto.getAbsoluteLimit().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Absolute limit cannot be positive");
        }
        account.setAbsoluteLimit(dto.getAbsoluteLimit());
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateDailyTransferLimit(String iban, SetDailyLimitRequestDTO dto) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        if (dto.getDailyTransferLimit() == null) {
            throw new BusinessRuleException("dailyTransferLimit is required");
        }
        if (dto.getDailyTransferLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("dailyTransferLimit cannot be negative");
        }
        account.setDailyTransferLimit(dto.getDailyTransferLimit());
        return accountRepository.save(account);
    }

    public Page<User> getAllCustomers(Pageable pageable) {
        return userRepository.findAllByRole(Role.CUSTOMER, pageable);
    }

    public Page<Account> getAccountsByCustomer(Long customerId, Pageable pageable) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        return accountRepository.findByUserId(user.getId(), pageable);
    }
}