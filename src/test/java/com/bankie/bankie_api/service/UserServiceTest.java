package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.UpdateProfileRequestDTO;
import com.bankie.bankie_api.dto.request.UserFilterDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.EmailAlreadyExistsException;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.policy.UserPolicy;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "alice@bankie.nl";

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock UserPolicy userPolicy;
    @InjectMocks UserService service;

    private final Pageable pageable = PageRequest.of(0, 20);

    private User customer() {
        return User.builder().id(1L).email(EMAIL).firstName("Alice").lastName("Liddell")
                .phoneNumber("+31600000000").role(Role.CUSTOMER).approved(true).build();
    }

    @Test
    void findByEmail_returnsUser() {
        User user = customer();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThat(service.findByEmail(EMAIL)).isSameAs(user);
    }

    @Test
    void findByEmail_missing_throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEmail(EMAIL))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getProfile_mapsUser() {
        User user = customer();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDto(user)).thenReturn(UserResponseDTO.builder().email(EMAIL).build());

        UserResponseDTO result = service.getProfile(EMAIL);

        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void findAll_statusNoAccounts_usesNoAccountsQuery() {
        when(userRepository.findByRoleAndNoAccounts(Role.CUSTOMER, pageable)).thenReturn(Page.empty(pageable));

        service.findAll("no-accounts", new UserFilterDTO(null), pageable);

        verify(userRepository).findByRoleAndNoAccounts(Role.CUSTOMER, pageable);
    }

    @Test
    void findAll_statusAllClosed_usesAllClosedQuery() {
        when(userRepository.findByRoleAndAllAccountsClosed(Role.CUSTOMER, pageable)).thenReturn(Page.empty(pageable));

        service.findAll("all-closed", new UserFilterDTO(null), pageable);

        verify(userRepository).findByRoleAndAllAccountsClosed(Role.CUSTOMER, pageable);
    }

    @Test
    void findAll_unknownStatus_throwsBadRequest() {
        assertThatThrownBy(() -> service.findAll("bogus", new UserFilterDTO(null), pageable))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void findAll_noStatusWithApprovedFilter_usesFindAllByApproved() {
        when(userRepository.findAllByApproved(eq(true), eq(pageable))).thenReturn(Page.empty(pageable));

        service.findAll(null, new UserFilterDTO(true), pageable);

        verify(userRepository).findAllByApproved(true, pageable);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAll_noStatusNoFilter_usesFindAll() {
        when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        service.findAll(null, new UserFilterDTO(null), pageable);

        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findAllByApproved(anyBoolean(), any());
    }

    @Test
    void updateProfile_changesEmailAndPhone() {
        User user = customer();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(UserResponseDTO.builder().build());

        service.updateProfile(EMAIL, new UpdateProfileRequestDTO("new@bankie.nl", "+31611111111"));

        verify(userPolicy).requireEmailAvailable("new@bankie.nl");
        assertThat(user.getEmail()).isEqualTo("new@bankie.nl");
        assertThat(user.getPhoneNumber()).isEqualTo("+31611111111");
    }

    @Test
    void updateProfile_ignoresBlankAndUnchangedFields() {
        User user = customer();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(UserResponseDTO.builder().build());

        // same email, blank phone -> no changes, no email-availability check
        service.updateProfile(EMAIL, new UpdateProfileRequestDTO(EMAIL, "  "));

        verify(userPolicy, never()).requireEmailAvailable(any());
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getPhoneNumber()).isEqualTo("+31600000000");
    }

    @Test
    void updateProfile_propagatesEmailConflict() {
        User user = customer();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        doThrow(new EmailAlreadyExistsException()).when(userPolicy).requireEmailAvailable("taken@bankie.nl");

        assertThatThrownBy(() -> service.updateProfile(EMAIL,
                new UpdateProfileRequestDTO("taken@bankie.nl", null)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }
}
