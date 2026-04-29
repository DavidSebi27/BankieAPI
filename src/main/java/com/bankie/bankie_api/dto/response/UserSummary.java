package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.model.User;
import com.bankie.bankie_api.model.enums.Role;

public record UserSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean approved
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isApproved()
        );
    }
}