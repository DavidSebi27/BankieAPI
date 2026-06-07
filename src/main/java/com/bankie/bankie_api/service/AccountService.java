package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.AuthContext;
import com.bankie.bankie_api.dto.request.AccountSearchFilterDTO;
import com.bankie.bankie_api.dto.request.CreateAccountRequestDTO;
import com.bankie.bankie_api.dto.request.SetAbsoluteLimitRequestDTO;
import com.bankie.bankie_api.dto.request.SetDailyLimitRequestDTO;
import com.bankie.bankie_api.dto.request.VerifyRecipientRequestDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.AccountNotFoundException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.mapper.AccountMapper;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.policy.AccountPolicy;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.util.IbanGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final UserMapper userMapper;
    private final AccountPolicy accountPolicy;
    private final IbanGenerator ibanGenerator;

    @Value("${bankie.account.default-daily-limit}")
    private BigDecimal defaultDailyLimit;

    // customerId = null  → customer sees own accounts, employee sees all
    // customerId = 123   → employee sees that specific customer's accounts
    public Page<AccountResponseDTO> getAccountsForUser(Long customerId, AuthContext authContext, Pageable pageable) {
        if (customerId != null) {
            User user = userRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
            return accountRepository.findByUserId(user.getId(), pageable)
                    .map(accountMapper::toResponseDto);
        }

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

    @Transactional(readOnly = true)
    public SearchAccountResponseDTO verifyRecipient(VerifyRecipientRequestDTO request) {
        String normalizedIban = request.iban().replaceAll("\\s", "").toUpperCase();
        String inFirst = request.firstName().trim();
        String inLast = request.lastName().trim();

        Account account = accountRepository.findById(normalizedIban)
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .filter(a -> a.getUser() != null
                        && a.getUser().getRole() == Role.CUSTOMER
                        && a.getUser().isApproved())
                .filter(a -> equalsTrimCi(a.getUser().getFirstName(), inFirst)
                        && equalsTrimCi(a.getUser().getLastName(), inLast))
                .orElseThrow(() -> new AccountNotFoundException(normalizedIban));

        return accountMapper.toSearchResponseDto(account);
    }


    @Transactional
    public List<AccountResponseDTO> approveCustomerAndCreateAccounts(Long customerId, CreateAccountRequestDTO dto) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        accountPolicy.requireCustomerRole(user);
        accountPolicy.requireNoExistingAccounts(accountRepository.existsByUser(user));
        accountPolicy.requireValidAbsoluteLimit(dto.getAbsoluteLimit());
        accountPolicy.requireValidDailyLimit(dto.getDailyTransferLimit());

        user.setApproved(true);
        userRepository.save(user);

        BigDecimal absoluteLimit = dto.getAbsoluteLimit() != null ? dto.getAbsoluteLimit() : BigDecimal.ZERO;
        BigDecimal dailyLimit = dto.getDailyTransferLimit() != null ? dto.getDailyTransferLimit() : defaultDailyLimit;

        Account checking = accountRepository.save(buildAccount(user, AccountType.CHECKING, absoluteLimit, dailyLimit));
        Account savings = accountRepository.save(buildAccount(user, AccountType.SAVINGS, BigDecimal.ZERO, dailyLimit));

        return List.of(accountMapper.toResponseDto(checking), accountMapper.toResponseDto(savings));
    }

    @Transactional
    public AccountResponseDTO closeAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        accountPolicy.requireAccountNotClosed(account);
        account.setStatus(AccountStatus.CLOSED);
        return accountMapper.toResponseDto(accountRepository.save(account));
    }

    @Transactional
    public AccountResponseDTO updateAbsoluteLimit(String iban, SetAbsoluteLimitRequestDTO dto) {
        accountPolicy.requireAbsoluteLimitPresent(dto.getAbsoluteLimit());
        accountPolicy.requireValidAbsoluteLimit(dto.getAbsoluteLimit());
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        account.setAbsoluteLimit(dto.getAbsoluteLimit());
        return accountMapper.toResponseDto(accountRepository.save(account));
    }

    @Transactional
    public AccountResponseDTO updateDailyTransferLimit(String iban, SetDailyLimitRequestDTO dto) {
        accountPolicy.requireDailyLimitPresent(dto.getDailyTransferLimit());
        accountPolicy.requireValidDailyLimit(dto.getDailyTransferLimit());
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        account.setDailyTransferLimit(dto.getDailyTransferLimit());
        return accountMapper.toResponseDto(accountRepository.save(account));
    }

    private Account buildAccount(User user, AccountType type, BigDecimal absoluteLimit, BigDecimal dailyLimit) {
        return Account.builder()
                .iban(ibanGenerator.generate())
                .type(type)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .absoluteLimit(absoluteLimit)
                .dailyTransferLimit(dailyLimit)
                .user(user)
                .build();
    }

    private Long currentUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();
    }

    private static boolean equalsTrimCi(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}