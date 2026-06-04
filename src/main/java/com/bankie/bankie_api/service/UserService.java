package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.UpdateProfileRequestDTO;
import com.bankie.bankie_api.dto.request.UserFilterDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.policy.UserPolicy;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserPolicy userPolicy;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public UserResponseDTO getProfile(String email) {
        return userMapper.toResponseDto(findByEmail(email));
    }

    public Page<UserResponseDTO> findAll(UserFilterDTO filter, Pageable pageable) {
        Page<User> users = filter.approved() != null
                ? userRepository.findAllByApproved(filter.approved(), pageable)
                : userRepository.findAll(pageable);
        return users.map(userMapper::toResponseDto);
    }

    @Transactional
    public UserResponseDTO updateProfile(String currentEmail, UpdateProfileRequestDTO dto) {
        User user = findByEmail(currentEmail);
        if (dto.email() != null && !dto.email().isBlank() && !dto.email().equals(user.getEmail())) {
            userPolicy.requireEmailAvailable(dto.email());
            user.setEmail(dto.email());
        }
        if (dto.phoneNumber() != null && !dto.phoneNumber().isBlank()) {
            user.setPhoneNumber(dto.phoneNumber());
        }
        return userMapper.toResponseDto(userRepository.save(user));
    }
}
