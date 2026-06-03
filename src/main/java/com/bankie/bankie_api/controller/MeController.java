package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.dto.request.UserRequestDTO;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.AccountMapper;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final UserMapper userMapper;

    @GetMapping
    public UserResponseDTO myProfile(@AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.toResponseDto(user);
    }

    @GetMapping("/accounts")
    public List<AccountResponseDTO> myAccounts(@AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return accountRepository.findByUser(user).stream()
                .map(accountMapper::toResponseDto)
                .toList();
    }

    @PatchMapping
    public ResponseEntity<UserResponseDTO> updateMe(
            @AuthenticationPrincipal String email,
            @RequestBody UserRequestDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) user.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) user.setPhoneNumber(dto.getPhoneNumber());
        userRepository.save(user);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }
}