package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionResponseDTO toResponseDto(Transaction transaction, String fromName, String toName){
        if (transaction == null) return null;

        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .fromIban(transaction.getFromIban())
                .fromName(fromName)
                .toIban(transaction.getToIban())
                .toName(toName)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .timestamp(transaction.getTimestamp())
                .initiatedBy(transaction.getInitiatedBy())
                .build();
    }
}
