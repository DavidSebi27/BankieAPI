package com.bankie.bankie_api.security;

import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-must-be-at-least-256-bits-or-32-bytes-long",
            60_000L);

    private User sampleUser() {
        return User.builder().id(7L).email("alice@bankie.nl").role(Role.CUSTOMER).build();
    }

    @Test
    void generatedTokenContainsUserClaims() {
        Claims claims = jwtService.parse(jwtService.generate(sampleUser()));

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("email", String.class)).isEqualTo("alice@bankie.nl");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void parseRejectsTamperedToken() {
        String token = jwtService.generate(sampleUser());
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }
}