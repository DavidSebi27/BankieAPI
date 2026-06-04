package com.bankie.bankie_api.dto.request;

import com.bankie.bankie_api.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterDTO {
    private Long initiatedBy;
    private Long customerId;
    private TransactionType type;
    private String iban;
    private LocalDateTime start;
    private LocalDateTime end;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
