package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        String fromIban,
        String toIban,
        BigDecimal amount,
        LocalDateTime timestamp,
        Long initiatedById,
        String initiatedByName
) {
    public static TransactionResponse from(Transaction tx, String initiatedByName) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getFromIban(),
                tx.getToIban(),
                tx.getAmount(),
                tx.getTimestamp(),
                tx.getInitiatedBy(),
                initiatedByName
        );
    }
}