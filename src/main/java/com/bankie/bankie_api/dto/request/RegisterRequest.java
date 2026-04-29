package com.bankie.bankie_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank @Pattern(regexp = "\\d{8,9}", message = "BSN must be 8 or 9 digits") String bsn,
        @NotBlank String phoneNumber
) {}