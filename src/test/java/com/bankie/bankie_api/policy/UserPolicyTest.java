package com.bankie.bankie_api.policy;

import com.bankie.bankie_api.exception.BsnAlreadyExistsException;
import com.bankie.bankie_api.exception.EmailAlreadyExistsException;
import com.bankie.bankie_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPolicyTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserPolicy policy;

    @Test
    void requireEmailAvailable_passesWhenEmailNotTaken() {
        when(userRepository.existsByEmail("free@bankie.nl")).thenReturn(false);

        assertThatCode(() -> policy.requireEmailAvailable("free@bankie.nl")).doesNotThrowAnyException();
    }

    @Test
    void requireEmailAvailable_throwsWhenEmailTaken() {
        when(userRepository.existsByEmail("taken@bankie.nl")).thenReturn(true);

        assertThatThrownBy(() -> policy.requireEmailAvailable("taken@bankie.nl"))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void requireBsnAvailable_passesWhenBsnNotTaken() {
        when(userRepository.existsByBsn("123456789")).thenReturn(false);

        assertThatCode(() -> policy.requireBsnAvailable("123456789")).doesNotThrowAnyException();
    }

    @Test
    void requireBsnAvailable_throwsWhenBsnTaken() {
        when(userRepository.existsByBsn("987654321")).thenReturn(true);

        assertThatThrownBy(() -> policy.requireBsnAvailable("987654321"))
                .isInstanceOf(BsnAlreadyExistsException.class);
    }
}
