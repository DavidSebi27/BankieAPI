package com.bankie.bankie_api.config;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
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

            User customer = User.builder()
                    .firstName("Giga")
                    .lastName("Chad")
                    .email("gigachad@gigabank.nl")
                    .password(passwordEncoder.encode("ILoveMoney123!"))
                    .bsn("069420666")
                    .phoneNumber("+31612345678")
                    .role(Role.CUSTOMER)
                    .approved(true)
                    .build();
            userRepository.save(customer);

            Account checking = Account.builder()
                    .iban("NL69INHO0420666999")
                    .type(AccountType.CHECKING)
                    .balance(new BigDecimal("1000.00"))
                    .status(AccountStatus.ACTIVE)
                    .absoluteLimit(new BigDecimal("-50.0"))
                    .dailyTransferLimit(new BigDecimal("500.0"))
                    .user(customer)
                    .build();

            Account savings = Account.builder()
                    .iban("NL42INHO0000000001")
                    .type(AccountType.SAVINGS)
                    .balance(new BigDecimal("5000.00"))
                    .status(AccountStatus.ACTIVE)
                    .absoluteLimit(new BigDecimal("0.0"))
                    .dailyTransferLimit(new BigDecimal("1000.0"))
                    .user(customer)
                    .build();

            accountRepository.save(checking);
            accountRepository.save(savings);

            Transaction initialTransfer = Transaction.builder()
                    .type(TransactionType.TRANSFER)
                    .fromIban(savings.getIban())
                    .toIban(checking.getIban())
                    .amount(new BigDecimal("250.0"))
                    .timestamp(LocalDateTime.now())
                    .initiatedBy(customer.getId())
                    .build();

            transactionRepository.save(initialTransfer);

            System.out.println("--- Database Seeded Successfully ---");
            System.out.println("Employee: admin@bankie.nl");
            System.out.println("Customer: gigachad@gigabank.nl");
        }
    }
}