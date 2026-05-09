package com.bankie.bankie_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {
    private String fromIban;
    private String toIban;
    private BigDecimal amount;
}
