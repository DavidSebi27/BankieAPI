package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.enums.TransactionType;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    public Page<TransactionResponseDTO> findAll(Long initiatedBy, TransactionType type, String iban,
                                                LocalDateTime start, LocalDateTime end,
                                                BigDecimal min, BigDecimal max,
                                                Pageable pageable, String email, boolean isEmployee) {
        List<String> ownerIbans = null;

        if (!isEmployee) {
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            ownerIbans = accountRepository.findByUser(currentUser).stream()
                    .map(Account::getIban)
                    .toList();

            if (ownerIbans.isEmpty()) return Page.empty(pageable);

            // Prevent a customer from filtering by an IBAN they don't own
            if (iban != null && ownerIbans.stream().noneMatch(i -> i.equalsIgnoreCase(iban))) {
                return Page.empty(pageable);
            }
        }

        Page<Transaction> transactions = transactionRepository.findAllFiltered(initiatedBy, ownerIbans, type, iban, start, end, min, max, pageable);

        return transactions.map(tx -> {
            // Extract names directly from the joined entities
            String fromName = (tx.getFromAccount() != null)
                    ? tx.getFromAccount().getUser().getFirstName() + " " + tx.getFromAccount().getUser().getLastName()
                    : "External/ATM";

            String toName = (tx.getToAccount() != null)
                    ? tx.getToAccount().getUser().getFirstName() + " " + tx.getToAccount().getUser().getLastName()
                    : "External/ATM";

            return transactionMapper.toResponseDto(tx, fromName, toName);
        });
    }

    @Transactional
    public TransactionResponseDTO transfer(TransferRequestDTO req, Authentication auth) {
        if (req.getFromIban().equals(req.getToIban())) {
            throw new BusinessRuleException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepository.findById(req.getFromIban())
                .orElseThrow(() -> new EntityNotFoundException("Source account not found"));
        Account toAccount = accountRepository.findById(req.getToIban())
                .orElseThrow(() -> new EntityNotFoundException("Destination account not found"));

        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!fromAccount.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessRuleException("You do not own this account");
        }
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Source account is not active");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Destination account is not active");
        }

        boolean isExternal = !toAccount.getUser().getId().equals(currentUser.getId());
        if (isExternal) {
            if (fromAccount.getType() != AccountType.CHECKING) {
                throw new BusinessRuleException("External transfers must originate from a checking account");
            }
            if (toAccount.getType() != AccountType.CHECKING) {
                throw new BusinessRuleException("External transfers must go to a checking account");
            }
        }

        BigDecimal amount = req.getAmount();
        if (fromAccount.getBalance().subtract(amount).compareTo(fromAccount.getAbsoluteLimit()) < 0) {
            throw new BusinessRuleException("Transfer would breach the absolute limit");
        }

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        BigDecimal alreadySentToday = transactionRepository.sumTodayOutgoing(req.getFromIban(), startOfToday);
        if (alreadySentToday.add(amount).compareTo(fromAccount.getDailyTransferLimit()) > 0) {
            throw new BusinessRuleException("Daily transfer limit exceeded");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction tx = Transaction.builder()
                .type(TransactionType.TRANSFER)
                .fromIban(req.getFromIban())
                .toIban(req.getToIban())
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .initiatedBy(currentUser.getId())
                .build();
        tx = transactionRepository.save(tx);

        String fromName = fromAccount.getUser().getFirstName() + " " + fromAccount.getUser().getLastName();
        String toName = toAccount.getUser().getFirstName() + " " + toAccount.getUser().getLastName();
        return transactionMapper.toResponseDto(tx, fromName, toName);
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
