package com.bankie.bankie_api.dto;

import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDTO {
    private String iban;
    private AccountType type;
    private Double balance;
    private String currency;
    private AccountStatus status;
    private Double absoluteLimit;
    private Double dailyTransferLimit;
    private Long ownerId;
}
