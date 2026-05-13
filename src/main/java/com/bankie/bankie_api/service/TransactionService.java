package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.mapper.TransactionMapper;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    public Page<TransactionResponseDTO> findAll(Long initiatedBy, TransactionType type, String iban,
                                                LocalDateTime start, LocalDateTime end,
                                                BigDecimal min, BigDecimal max,
                                                Pageable pageable, Authentication auth) {
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        // If not employee, they can ONLY see their own transactions
        if (!isEmployee) {
            User currentUser = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            initiatedBy = currentUser.getId();
        }

        Page<Transaction> transactions = transactionRepository.findAllFiltered(initiatedBy, type, iban, start, end, min, max, pageable);

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
}
