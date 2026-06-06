package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.PageResponse;
import com.bankie.bankie_api.dto.request.UserFilterDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints. Restricted to employees unless noted.")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
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
            @ParameterObject UserFilterDTO filter,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(userService.findAll(filter, pageable)));
    }
}
