package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.AtmRequestDTO;
import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.mapper.TransactionMapper;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    public Page<TransactionResponseDTO> findAll(Pageable pageable) {
        return mapWithNames(transactionRepository.findAll(pageable));
    }

    public Page<TransactionResponseDTO> findByCustomer(Long customerId, Pageable pageable) {
        User customer = userRepository.findById(customerId)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<String> ibans = accountRepository.findByUser(customer).stream()
                .map(Account::getIban)
                .toList();

        if (ibans.isEmpty()) return Page.empty(pageable);

        return mapWithNames(transactionRepository.findByIbanIn(ibans, pageable));
    }

    @Transactional
    public TransactionResponseDTO transfer(TransferRequestDTO request, String initiatorEmail) {
        if (request.getFromIban().equals(request.getToIban())) {
            throw new BusinessRuleException("Source and destination accounts must be different");
        }

        Account source = accountRepository.findById(request.getFromIban())
                .orElseThrow(() -> new BusinessRuleException("Source account not found"));
        Account destination = accountRepository.findById(request.getToIban())
                .orElseThrow(() -> new BusinessRuleException("Destination account not found"));

        requireActiveCustomerChecking(source, "Source");
        requireActiveCustomerChecking(destination, "Destination");

        if (!Objects.equals(source.getCurrency(), destination.getCurrency())) {
            throw new BusinessRuleException("Accounts must share the same currency");
        }

        BigDecimal amount = request.getAmount();
        BigDecimal newBalance = source.getBalance().subtract(amount);
        if (newBalance.compareTo(source.getAbsoluteLimit()) < 0) {
            throw new BusinessRuleException("Transfer would breach the source account's absolute limit");
        }

        enforceDailyLimit(source, amount, "Transfer");

        User initiator = resolveInitiator(initiatorEmail);

        source.setBalance(newBalance);
        destination.setBalance(destination.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .type(TransactionType.TRANSFER)
                .fromIban(source.getIban())
                .toIban(destination.getIban())
                .amount(amount)
                .currency(source.getCurrency())
                .timestamp(LocalDateTime.now())
                .initiatedBy(initiator.getId())
                .build());

        return toDto(tx, initiator);
    }

    @Transactional
    public TransactionResponseDTO withdraw(AtmRequestDTO request, String initiatorEmail) {
        Account account = accountRepository.findById(request.getIban())
                .orElseThrow(() -> new BusinessRuleException("Account not found"));
        requireActiveCustomerChecking(account, "Account");

        BigDecimal amount = request.getAmount();
        BigDecimal newBalance = account.getBalance().subtract(amount);
        if (newBalance.compareTo(account.getAbsoluteLimit()) < 0) {
            throw new BusinessRuleException("Withdrawal would breach the account's absolute limit");
        }
        enforceDailyLimit(account, amount, "Withdrawal");

        User initiator = resolveInitiator(initiatorEmail);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .type(TransactionType.WITHDRAWAL)
                .fromIban(account.getIban())
                .amount(amount)
                .currency(account.getCurrency())
                .timestamp(LocalDateTime.now())
                .initiatedBy(initiator.getId())
                .build());

        return toDto(tx, initiator);
    }

    @Transactional
    public TransactionResponseDTO deposit(AtmRequestDTO request, String initiatorEmail) {
        Account account = accountRepository.findById(request.getIban())
                .orElseThrow(() -> new BusinessRuleException("Account not found"));
        requireActiveCustomerChecking(account, "Account");

        BigDecimal amount = request.getAmount();
        enforceDailyLimit(account, amount, "Deposit");

        User initiator = resolveInitiator(initiatorEmail);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .type(TransactionType.DEPOSIT)
                .toIban(account.getIban())
                .amount(amount)
                .currency(account.getCurrency())
                .timestamp(LocalDateTime.now())
                .initiatedBy(initiator.getId())
                .build());

        return toDto(tx, initiator);
    }

    private void enforceDailyLimit(Account account, BigDecimal amount, String label) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal usedToday = transactionRepository.sumDailyMovementsSince(account.getIban(), startOfDay);
        if (usedToday.add(amount).compareTo(account.getDailyTransferLimit()) > 0) {
            throw new BusinessRuleException(label + " exceeds the account's daily transfer limit");
        }
    }

    private User resolveInitiator(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("Initiator not found"));
    }

    private TransactionResponseDTO toDto(Transaction tx, User initiator) {
        TransactionResponseDTO dto = transactionMapper.toResponseDto(tx);
        dto.setInitiatedByName(initiator.getFirstName() + " " + initiator.getLastName());
        return dto;
    }

    private void requireActiveCustomerChecking(Account account, String label) {
        if (account.getType() != AccountType.CHECKING) {
            throw new BusinessRuleException(label + " account must be a checking account");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(label + " account is not active");
        }
        if (account.getUser() == null || account.getUser().getRole() != Role.CUSTOMER) {
            throw new BusinessRuleException(label + " account must belong to a customer");
        }
    }

    private Page<TransactionResponseDTO> mapWithNames(Page<Transaction> page) {
        Set<Long> userIds = page.stream()
                .map(Transaction::getInitiatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> names = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getFirstName() + " " + u.getLastName()));

        return page.map(tx -> {
            TransactionResponseDTO dto = transactionMapper.toResponseDto(tx);
            dto.setInitiatedByName(names.get(tx.getInitiatedBy()));
            return dto;
        });
    }
}
