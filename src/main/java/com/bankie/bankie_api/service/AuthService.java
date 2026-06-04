package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.LoginRequest;
import com.bankie.bankie_api.dto.request.RegisterRequest;
import com.bankie.bankie_api.dto.response.LoginResponse;
import com.bankie.bankie_api.dto.response.UserSummary;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.mapper.UserMapper;
import com.bankie.bankie_api.policy.UserPolicy;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserPolicy userPolicy;

    @Transactional
    public UserSummary register(RegisterRequest request) {
        userPolicy.requireEmailAvailable(request.email());
        userPolicy.requireBsnAvailable(request.bsn());

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.password()));
        return userMapper.toSummary(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return userMapper.toLoginResponse(user, jwtService.generate(user));
    }
}
