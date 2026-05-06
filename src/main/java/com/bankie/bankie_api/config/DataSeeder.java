package com.bankie.bankie_api.config;

import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("employee@bankie.test")) return;

        User employee = userRepository.save(User.builder()
                .firstName("Test")
                .lastName("Employee")
                .email("employee@bankie.test")
                .password(passwordEncoder.encode("Employee123!"))
                .bsn("123456789")
                .phoneNumber("0612345678")
                .role(Role.EMPLOYEE)
                .approved(true)
                .build());

        User alice = userRepository.save(User.builder()
                .firstName("Alice")
                .lastName("Janssen")
                .email("alice@bankie.test")
                .password(passwordEncoder.encode("Alice123!"))
                .bsn("234567890")
                .phoneNumber("0611111111")
                .role(Role.CUSTOMER)
                .approved(true)
                .build());

        User bob = userRepository.save(User.builder()
                .firstName("Bob")
                .lastName("de Vries")
                .email("bob@bankie.test")
                .password(passwordEncoder.encode("Bob123!"))
                .bsn("345678901")
                .phoneNumber("0622222222")
                .role(Role.CUSTOMER)
                .approved(true)
                .build());

        String aliceIban = "NL01BANK0000000001";
        String bobIban   = "NL01BANK0000000002";
        LocalDateTime now = LocalDateTime.now();

        transactionRepository.saveAll(List.of(
                Transaction.builder()
                        .type(TransactionType.DEPOSIT)
                        .toIban(aliceIban)
                        .amount(new BigDecimal("250.00"))
                        .timestamp(now.minusDays(3))
                        .initiatedBy(alice.getId())
                        .build(),
                Transaction.builder()
                        .type(TransactionType.TRANSFER)
                        .fromIban(aliceIban)
                        .toIban(bobIban)
                        .amount(new BigDecimal("75.50"))
                        .timestamp(now.minusDays(2).minusHours(4))
                        .initiatedBy(alice.getId())
                        .build(),
                Transaction.builder()
                        .type(TransactionType.WITHDRAWAL)
                        .fromIban(bobIban)
                        .amount(new BigDecimal("40.00"))
                        .timestamp(now.minusDays(2))
                        .initiatedBy(bob.getId())
                        .build(),
                Transaction.builder()
                        .type(TransactionType.TRANSFER)
                        .fromIban(bobIban)
                        .toIban(aliceIban)
                        .amount(new BigDecimal("125.00"))
                        .timestamp(now.minusDays(1).minusHours(7))
                        .initiatedBy(employee.getId())
                        .build(),
                Transaction.builder()
                        .type(TransactionType.DEPOSIT)
                        .toIban(bobIban)
                        .amount(new BigDecimal("1000.00"))
                        .timestamp(now.minusDays(1))
                        .initiatedBy(bob.getId())
                        .build(),
                Transaction.builder()
                        .type(TransactionType.TRANSFER)
                        .fromIban(aliceIban)
                        .toIban(bobIban)
                        .amount(new BigDecimal("12.34"))
                        .timestamp(now.minusHours(2))
                        .initiatedBy(alice.getId())
                        .build()
        ));
    }
}