package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.mapper.TransactionMapper;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public TransactionResponseDTO performTransfer(TransferRequestDTO dto, Authentication authentication) {
        Account from = accountRepository.findById(dto.getFromIban())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + dto.getFromIban()));
        Account to = accountRepository.findById(dto.getToIban())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + dto.getToIban()));

        if (from.getStatus() == AccountStatus.CLOSED || to.getStatus() == AccountStatus.CLOSED)
            throw new BusinessRuleException("Cannot transfer to/from a closed account");

        if (from.getBalance().subtract(dto.getAmount()).compareTo(from.getAbsoluteLimit()) < 0)
            throw new BusinessRuleException("Transfer would breach the absolute limit");

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        BigDecimal transferredToday = transactionRepository.sumTransfersByIbanSince(from.getIban(), startOfDay);
        if (transferredToday.add(dto.getAmount()).compareTo(from.getDailyTransferLimit()) > 0)
            throw new BusinessRuleException("Daily transfer limit exceeded");

        from.setBalance(from.getBalance().subtract(dto.getAmount()));
        to.setBalance(to.getBalance().add(dto.getAmount()));
        accountRepository.save(from);
        accountRepository.save(to);

        User initiator = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Transaction tx = Transaction.builder()
                .type(TransactionType.TRANSFER)
                .fromIban(from.getIban())
                .toIban(to.getIban())
                .amount(dto.getAmount())
                .timestamp(LocalDateTime.now())
                .initiatedBy(initiator.getId())
                .build();

        return transactionMapper.toResponseDto(transactionRepository.save(tx));
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