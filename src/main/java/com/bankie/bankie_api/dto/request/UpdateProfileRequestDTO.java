package com.bankie.bankie_api.dto.request;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequestDTO(
        @Email String email,
        String phoneNumber
) {}
