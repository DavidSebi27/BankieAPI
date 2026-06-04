package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.UserFilterDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
}
