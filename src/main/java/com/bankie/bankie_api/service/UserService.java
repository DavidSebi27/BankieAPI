package com.bankie.bankie_api.service;

import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public Page findAll(Boolean approved, Pageable pageable) {
        if (approved != null) {
            return userRepository.findAllByApproved(approved, pageable);
        }
        return userRepository.findAll(pageable);
    }
}
