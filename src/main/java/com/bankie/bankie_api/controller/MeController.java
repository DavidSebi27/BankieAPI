package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.request.UpdateProfileRequestDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public UserResponseDTO myProfile(@AuthenticationPrincipal String email) {
        return userService.getProfile(email);
    }

    @PatchMapping
    public UserResponseDTO updateMyProfile(@AuthenticationPrincipal String email,
                                           @Valid @RequestBody UpdateProfileRequestDTO dto) {
        return userService.updateProfile(email, dto);
    }
}
