package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.PageResponse;
import com.bankie.bankie_api.dto.request.UpdateProfileRequestDTO;
import com.bankie.bankie_api.dto.request.UserFilterDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints. Restricted to employees unless noted.")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "List users (employee only)",
            description = "Returns a paginated list of users. Use status=no-accounts for customers without accounts, status=all-closed for customers with all accounts closed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of users."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token."),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have the EMPLOYEE role.")
    })
    public ResponseEntity<PageResponse<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String status,
            @ParameterObject UserFilterDTO filter,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(userService.findAll(status, filter, pageable)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get own profile", description = "Returns the profile of the authenticated user. Available to any authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile of the authenticated user."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token.")
    })
    public ResponseEntity<UserResponseDTO> myProfile(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.getProfile(email));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update own profile", description = "Updates the profile of the authenticated user. Available to any authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated profile of the authenticated user."),
            @ApiResponse(responseCode = "400", description = "Invalid request body."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token.")
    })
    public ResponseEntity<UserResponseDTO> updateMyProfile(@AuthenticationPrincipal String email,
                                                           @Valid @RequestBody UpdateProfileRequestDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(email, dto));
    }
}
