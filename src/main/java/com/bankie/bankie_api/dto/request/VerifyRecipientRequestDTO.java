package com.bankie.bankie_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyRecipientRequestDTO(
        @NotBlank String iban,
        @NotBlank String firstName,
        @NotBlank String lastName
) {}
