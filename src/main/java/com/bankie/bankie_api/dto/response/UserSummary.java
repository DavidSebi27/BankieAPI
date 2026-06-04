package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.enums.Role;

public record UserSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean approved
) {}
