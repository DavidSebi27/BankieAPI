package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    private Long id;
    private TransactionType type;
    private String fromIban;
    private String fromName;
    private String toIban;
    private String toName;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
    private Long initiatedBy;
}
