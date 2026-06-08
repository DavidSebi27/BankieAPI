package com.bankie.bankie_api.security;

import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    private static final String EMAIL = "alice@bankie.nl";

    @Mock UserRepository userRepository;
    @InjectMocks UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_returnsUserDetailsWithRoleAuthority() {
        User user = User.builder().email(EMAIL).password("HASHED").role(Role.EMPLOYEE).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(EMAIL);

        assertThat(details.getUsername()).isEqualTo(EMAIL);
        assertThat(details.getPassword()).isEqualTo("HASHED");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void loadUserByUsername_missing_throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
