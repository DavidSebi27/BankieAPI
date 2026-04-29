package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.LoginRequest;
import com.bankie.bankie_api.dto.request.RegisterRequest;
import com.bankie.bankie_api.dto.response.LoginResponse;
import com.bankie.bankie_api.dto.response.UserSummary;
import com.bankie.bankie_api.exception.BsnAlreadyExistsException;
import com.bankie.bankie_api.exception.EmailAlreadyExistsException;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private RegisterRequest validRequest() {
        return new RegisterRequest("John", "Doe", "john@bankie.nl", "secret12", "123456789", "+31600000000");
    }

    @Test
    void register_persistsCustomerWithHashedPasswordAndUnapproved() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByBsn(any())).thenReturn(false);
        when(passwordEncoder.encode("secret12")).thenReturn("HASHED");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserSummary result = authService.register(validRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("HASHED");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.isApproved()).isFalse();
        assertThat(result.email()).isEqualTo("john@bankie.nl");
        assertThat(result.approved()).isFalse();
    }

    @Test
    void register_throwsConflictWhenEmailTaken() {
        when(userRepository.existsByEmail("john@bankie.nl")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(validRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsConflictWhenBsnTaken() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByBsn("123456789")).thenReturn(true);

        assertThrows(BsnAlreadyExistsException.class, () -> authService.register(validRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_authenticatesAndReturnsTokenWithRoleAndApprovalFlag() {
        User user = User.builder().id(1L).email("john@bankie.nl").role(Role.CUSTOMER).approved(false).build();
        when(userRepository.findByEmail("john@bankie.nl")).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("john@bankie.nl", "secret12"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(response.approved()).isFalse();
    }

    @Test
    void login_propagatesBadCredentials() {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("john@bankie.nl", "wrong")));
    }
}