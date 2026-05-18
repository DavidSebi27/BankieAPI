package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.request.LoginRequest;
import com.bankie.bankie_api.dto.request.RegisterRequest;
import com.bankie.bankie_api.dto.response.LoginResponse;
import com.bankie.bankie_api.dto.response.UserSummary;
import com.bankie.bankie_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Customer self-registration and login for all user types.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new customer",
            description = "Creates a new customer account. No bank accounts are created yet — an employee must approve first."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer successfully registered."),
            @ApiResponse(responseCode = "400", description = "Validation error — one or more fields failed validation."),
            @ApiResponse(responseCode = "409", description = "Email address or BSN is already in use.")
    })
    public UserSummary register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate and obtain a JWT",
            description = "Validates credentials and returns a signed JWT. Include it as: Authorization: Bearer <token>."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful. Returns the JWT and the user's role."),
            @ApiResponse(responseCode = "401", description = "The email or password is incorrect.")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}