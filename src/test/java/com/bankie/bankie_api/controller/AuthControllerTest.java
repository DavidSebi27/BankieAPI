package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.request.LoginRequest;
import com.bankie.bankie_api.dto.request.RegisterRequest;
import com.bankie.bankie_api.dto.response.LoginResponse;
import com.bankie.bankie_api.dto.response.UserSummary;
import com.bankie.bankie_api.exception.EmailAlreadyExistsException;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.security.JwtService;
import com.bankie.bankie_api.service.AuthService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;

    @Test
    void register_returns201AndUserSummary() throws Exception {
        var request = new RegisterRequest("John", "Doe", "john@bankie.nl", "secret12", "123456789", "+31600000000");
        when(authService.register(any())).thenReturn(
                new UserSummary(1L, "John", "Doe", "john@bankie.nl", Role.CUSTOMER, false));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@bankie.nl"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void register_returns400OnInvalidPayload() throws Exception {
        String invalid = """
                {"firstName":"","lastName":"","email":"not-an-email","password":"x","bsn":"abc","phoneNumber":""}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns409OnDuplicateEmail() throws Exception {
        var request = new RegisterRequest("John", "Doe", "john@bankie.nl", "secret12", "123456789", "+31600000000");
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returns200WithToken() throws Exception {
        var request = new LoginRequest("john@bankie.nl", "secret12");
        when(authService.login(any())).thenReturn(new LoginResponse("jwt-token", Role.CUSTOMER, true));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    void login_returns401OnBadCredentials() throws Exception {
        var request = new LoginRequest("john@bankie.nl", "wrong");
        when(authService.login(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}