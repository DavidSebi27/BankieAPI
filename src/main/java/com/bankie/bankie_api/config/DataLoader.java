package com.bankie.bankie_api.config;

import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {

            User admin = User.builder()
                    .firstName("Bankie")
                    .lastName("Employee")
                    .email("admin@bankie.nl")
                    .password(passwordEncoder.encode("Admin123!"))
                    .bsn("123456789")
                    .phoneNumber("+31600000000")
                    .role(Role.EMPLOYEE)
                    .approved(true)
                    .build();

            userRepository.save(admin);

            System.out.println("--- Database Seeded: Default Employee created (admin@bankie.nl) ---");
        }
    }
}