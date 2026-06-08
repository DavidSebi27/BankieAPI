package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.request.UpdateProfileRequestDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.security.JwtService;
import com.bankie.bankie_api.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final String EMAIL = "alice@bankie.nl";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    // JwtAuthFilter sets the principal to the user's email (a String), which
    // @AuthenticationPrincipal String email then resolves from.
    private final Authentication customer = new UsernamePasswordAuthenticationToken(
            EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(customer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserResponseDTO userDto() {
        return UserResponseDTO.builder().id(1L).firstName("Alice").lastName("Liddell")
                .email(EMAIL).phoneNumber("+31600000000").role(Role.CUSTOMER).approved(true).build();
    }

    @Test
    void getAllUsers_returnsPageResponse() throws Exception {
        when(userService.findAll(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(userDto())));

        mockMvc.perform(get("/users").principal(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(EMAIL))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void myProfile_returnsOwnProfile() throws Exception {
        when(userService.getProfile(EMAIL)).thenReturn(userDto());

        mockMvc.perform(get("/users/me").principal(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void updateMyProfile_returnsUpdatedProfile() throws Exception {
        when(userService.updateProfile(eq(EMAIL), any())).thenReturn(userDto());

        mockMvc.perform(patch("/users/me")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequestDTO("alice@bankie.nl", "+31611111111"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void updateMyProfile_invalidEmail_isBadRequest() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }
}
