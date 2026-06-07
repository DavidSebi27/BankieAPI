package com.bankie.bankie_api.dto;

import org.springframework.security.core.Authentication;

public record AuthContext(String email, boolean isEmployee) {
    public static AuthContext from(Authentication authentication) {
        boolean isEmployee = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
        return new AuthContext(authentication.getName(), isEmployee);
    }
}
