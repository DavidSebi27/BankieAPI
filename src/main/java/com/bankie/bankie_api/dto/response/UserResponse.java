package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String bsn,
        String phoneNumber,
        Role role,
        boolean approved
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBsn(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isApproved()
        );
    }
}