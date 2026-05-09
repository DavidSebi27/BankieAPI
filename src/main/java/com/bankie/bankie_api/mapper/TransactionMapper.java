package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionResponseDTO toResponseDto(Transaction transaction){
        if (transaction == null) return null;

        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .fromIban(transaction.getFromIban())
                .toIban(transaction.getToIban())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .timestamp(transaction.getTimestamp())
                .initiatedBy(transaction.getInitiatedBy())
                .build();
    }
}
