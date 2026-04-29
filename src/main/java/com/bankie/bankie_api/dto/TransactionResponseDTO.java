package com.bankie.bankie_api.dto;

import com.bankie.bankie_api.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    private Long id;
    private TransactionType type;
    private String fromIban;
    private String toIban;
    private Double amount;
    private String currency;
    private LocalDateTime timestamp;
    private Long initiatedBy;
}
