package com.bankie.bankie_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AccountSearchFilterDTO(
        @NotBlank String firstName,
        @NotBlank String lastName
) {}
