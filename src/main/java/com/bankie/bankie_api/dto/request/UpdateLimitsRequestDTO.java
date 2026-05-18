package com.bankie.bankie_api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLimitsRequestDTO {
    private BigDecimal absoluteLimit;
    private BigDecimal dailyTransferLimit;
}