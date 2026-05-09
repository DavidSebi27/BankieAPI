package com.bankie.bankie_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequestDTO {
    private BigDecimal absoluteLimit;
    private BigDecimal dailyTransferLimit;
}
