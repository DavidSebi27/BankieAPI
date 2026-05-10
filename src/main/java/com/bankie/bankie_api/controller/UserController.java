package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userService.findByEmail(email);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')") // Restrict to employees as per spec
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) Boolean approved,
            @ParameterObject Pageable pageable) {

        Page<User> users = userService.findAll(approved, pageable);

        // Convert the Page of Entities into a Page of DTOs
        Page<UserResponseDTO> response = users.map(userMapper::toResponseDto);

        return ResponseEntity.ok(response);
    }
}
