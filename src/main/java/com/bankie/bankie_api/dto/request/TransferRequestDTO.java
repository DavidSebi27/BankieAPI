package com.bankie.bankie_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {
    @NotBlank
    private String fromIban;

    @NotBlank
    private String toIban;

    @NotNull
    @Positive
    private BigDecimal amount;
}
