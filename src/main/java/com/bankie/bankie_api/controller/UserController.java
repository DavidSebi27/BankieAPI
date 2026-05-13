package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.dto.PageResponse;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints. Restricted to employees unless noted.")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')") // Restrict to employees as per spec
    @Operation(
            summary = "List all users (employee only)",
            description = "Returns a paginated list of all customers. Use approved=false to find customers awaiting account creation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of users."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token."),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have the EMPLOYEE role.")
    })
    public ResponseEntity<PageResponse<UserResponseDTO>> getAllUsers(
            @Parameter(description = "Filter by approval status. false = pending, true = approved. Omit to return all.")
            @RequestParam(required = false) Boolean approved,
            @ParameterObject Pageable pageable) {

        Page<User> users = userService.findAll(approved, pageable);

        // Convert the Page of Entities into a Page of DTOs
        PageResponse<UserResponseDTO> response = PageResponse.from(users.map(userMapper::toResponseDto));

        return ResponseEntity.ok(response);
    }
}