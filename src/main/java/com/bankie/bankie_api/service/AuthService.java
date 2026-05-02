package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.LoginRequest;
import com.bankie.bankie_api.dto.request.RegisterRequest;
import com.bankie.bankie_api.dto.response.LoginResponse;
import com.bankie.bankie_api.dto.response.UserSummary;
import com.bankie.bankie_api.exception.BsnAlreadyExistsException;
import com.bankie.bankie_api.exception.EmailAlreadyExistsException;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
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

    @Transactional
    public UserSummary register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) throw new EmailAlreadyExistsException();
        if (userRepository.existsByBsn(request.bsn())) throw new BsnAlreadyExistsException();

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .bsn(request.bsn())
                .phoneNumber(request.phoneNumber())
                .role(Role.CUSTOMER)
                .build();

        return UserSummary.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return new LoginResponse(jwtService.generate(user), user.getRole(), user.isApproved());
    }
}