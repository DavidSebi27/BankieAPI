package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.AtmRequestDTO;
import com.bankie.bankie_api.dto.request.TransactionFilterDTO;
import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.mapper.TransactionMapper;
import com.bankie.bankie_api.policy.TransactionPolicy;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
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
    private final TransactionPolicy policy;

    public Page<TransactionResponseDTO> findAll(TransactionFilterDTO filter, Pageable pageable,
                                                String email, boolean isEmployee) {
        List<String> ownerIbans = null;
        Long initiatedBy = filter.getInitiatedBy();

        if (!isEmployee) {
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            ownerIbans = accountRepository.findByUser(currentUser).stream()
                    .map(Account::getIban)
                    .toList();

            if (filter.getIban() != null && ownerIbans.stream().noneMatch(i -> i.equalsIgnoreCase(filter.getIban()))) {
                return Page.empty(pageable);
            }

            initiatedBy = null;
        }

        Page<Transaction> transactions = transactionRepository.findAllFiltered(
                initiatedBy, ownerIbans, filter.getType(), filter.getIban(),
                filter.getStart(), filter.getEnd(), filter.getMinAmount(), filter.getMaxAmount(), pageable);

        return transactions.map(transactionMapper::toResponseDto);
    }

    public Page<TransactionResponseDTO> findByCustomer(Long customerId, Pageable pageable) {
        User customer = userRepository.findById(customerId)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<String> ibans = accountRepository.findByUser(customer).stream()
                .map(Account::getIban)
                .toList();

        if (ibans.isEmpty()) return Page.empty(pageable);

        return mapWithInitiatorNames(transactionRepository.findByIbanIn(ibans, pageable));
    }

    @Transactional
    public TransactionResponseDTO transfer(TransferRequestDTO request, String initiatorEmail) {
        policy.requireDifferentAccounts(request.getFromIban(), request.getToIban());

        Account source = accountRepository.findById(request.getFromIban())
                .orElseThrow(() -> new BusinessRuleException("Source account not found"));
        Account destination = accountRepository.findById(request.getToIban())
                .orElseThrow(() -> new BusinessRuleException("Destination account not found"));

        policy.requireActiveCustomerChecking(source, "Source");
        policy.requireActiveCustomerChecking(destination, "Destination");
        policy.requireSameCurrency(source, destination);

        BigDecimal amount = request.getAmount();
        BigDecimal newBalance = source.getBalance().subtract(amount);
        policy.requireWithinAbsoluteLimit(source, newBalance, "Transfer");
        policy.requireWithinDailyLimit(source, amount, dailyMovements(source), "Transfer");

        User initiator = resolveInitiator(initiatorEmail);

        source.setBalance(newBalance);
        destination.setBalance(destination.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction tx = transactionRepository.save(
                transactionMapper.toTransferEntity(request, initiator.getId(), source.getCurrency()));

        return transactionMapper.toResponseDto(tx, initiator);
    }

    @Transactional
    public TransactionResponseDTO withdraw(AtmRequestDTO request, String initiatorEmail) {
        Account account = accountRepository.findById(request.getIban())
                .orElseThrow(() -> new BusinessRuleException("Account not found"));
        policy.requireActiveCustomerChecking(account, "Account");

        BigDecimal amount = request.getAmount();
        BigDecimal newBalance = account.getBalance().subtract(amount);
        policy.requireWithinAbsoluteLimit(account, newBalance, "Withdrawal");
        policy.requireWithinDailyLimit(account, amount, dailyMovements(account), "Withdrawal");

        User initiator = resolveInitiator(initiatorEmail);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = transactionRepository.save(
                transactionMapper.toWithdrawalEntity(request, initiator.getId(), account.getCurrency()));

        return transactionMapper.toResponseDto(tx, initiator);
    }

    @Transactional
    public TransactionResponseDTO deposit(AtmRequestDTO request, String initiatorEmail) {
        Account account = accountRepository.findById(request.getIban())
                .orElseThrow(() -> new BusinessRuleException("Account not found"));
        policy.requireActiveCustomerChecking(account, "Account");

        BigDecimal amount = request.getAmount();
        policy.requireWithinDailyLimit(account, amount, dailyMovements(account), "Deposit");

        User initiator = resolveInitiator(initiatorEmail);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = transactionRepository.save(
                transactionMapper.toDepositEntity(request, initiator.getId(), account.getCurrency()));

        return transactionMapper.toResponseDto(tx, initiator);
    }

    private BigDecimal dailyMovements(Account account) {
        return transactionRepository.sumDailyMovementsSince(account.getIban(), LocalDate.now().atStartOfDay());
    }

    private User resolveInitiator(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("Initiator not found"));
    }

    private Page<TransactionResponseDTO> mapWithInitiatorNames(Page<Transaction> page) {
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
