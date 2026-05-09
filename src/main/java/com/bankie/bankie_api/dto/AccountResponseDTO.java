package com.bankie.bankie_api.dto;

import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDTO {
    private String iban;
    private AccountType type;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private BigDecimal absoluteLimit;
    private BigDecimal dailyTransferLimit;
    private Long userId;
}
