package com.bankie.bankie_api.config;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.*;
import com.bankie.bankie_api.repository.*;
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
            seedDatabase();
            System.out.println("--- Database Seeded Successfully ---");
        }
    }

    private void seedDatabase() {
        User admin = createUser("Bankie", "Employee", "admin@bankie.nl", "Admin123!", "123456789", Role.EMPLOYEE, true);
        User ryan = createUser("Ryan", "Reynolds", "ryan@deadpool.com", "GreenLanternSucks1!", "111222333", Role.CUSTOMER, true);
        User taylor = createUser("Taylor", "Swift", "taylor@eras.com", "ShakeItOff123!", "444555666", Role.CUSTOMER, true);
        User keanu = createUser("Keanu", "Reeves", "keanu@matrix.com", "YouAreBreathtaking1!", "777888999", Role.CUSTOMER, true);
        User pete = createUser("Pending", "Pete", "pete@waiting.nl", "PetePass123!", "000111222", Role.CUSTOMER, false);

        createAccount("NL69REYN0420666999", AccountType.CHECKING, "1000000.00", "5000.00", ryan);
        createAccount("NL42REYN0000000001", AccountType.SAVINGS, "5000000.00", "10000.00", ryan);
        createAccount("NL13SWIF0123456789", AccountType.CHECKING, "2500000.50", "10000.00", taylor);
        createAccount("NL02REEV9876543210", AccountType.CHECKING, "150000.00", "2000.00", keanu);

        seedTransactions(ryan, taylor, keanu);
    }

    private User createUser(String first, String last, String email, String pass, String bsn, Role role, boolean approved) {
        User user = User.builder()
                .firstName(first).lastName(last).email(email)
                .password(passwordEncoder.encode(pass)).bsn(bsn)
                .phoneNumber("+31600000000").role(role).approved(approved).build();
        return userRepository.save(user);
    }

    private void createAccount(String iban, AccountType type, String balance, String limit, User user) {
        Account acc = Account.builder()
                .iban(iban).type(type).balance(new BigDecimal(balance))
                .status(AccountStatus.ACTIVE).absoluteLimit(BigDecimal.ZERO)
                .dailyTransferLimit(new BigDecimal(limit)).user(user).build();
        accountRepository.save(acc);
    }

    private void seedTransactions(User ryan, User taylor, User keanu) {
        String ryanIban = "NL69REYN0420666999";
        String taylorIban = "NL13SWIF0123456789";
        String keanuIban = "NL02REEV9876543210";

        addTx(TransactionType.TRANSFER, ryanIban, taylorIban, "500.00", 5, ryan.getId());
        addTx(TransactionType.TRANSFER, ryanIban, taylorIban, "600.30", 5, ryan.getId());
        addTx(TransactionType.TRANSFER, ryanIban, taylorIban, "700.20", 5, ryan.getId());
        addTx(TransactionType.TRANSFER, taylorIban, ryanIban, "1200.00", 4, taylor.getId());
        addTx(TransactionType.TRANSFER, keanuIban, ryanIban, "50.00", 3, keanu.getId());
        addTx(TransactionType.DEPOSIT, null, keanuIban, "10000.00", 2, keanu.getId());
        addTx(TransactionType.WITHDRAWAL, ryanIban, null, "200.00", 1, ryan.getId());
        addTx(TransactionType.TRANSFER, ryanIban, keanuIban, "300.00", 0, ryan.getId(), 10); // Hours ago
        addTx(TransactionType.TRANSFER, taylorIban, keanuIban, "15.00", 0, taylor.getId(), 8);
        addTx(TransactionType.TRANSFER, keanuIban, taylorIban, "100.00", 0, keanu.getId(), 5);
        addTx(TransactionType.TRANSFER, ryanIban, taylorIban, "45.00", 0, ryan.getId(), 2);
        addTx(TransactionType.WITHDRAWAL, taylorIban, null, "100.00", 0, taylor.getId(), 1);
    }

    private void addTx(TransactionType type, String from, String to, String amt, int daysAgo, Long by) {
        addTx(type, from, to, amt, daysAgo, by, 0);
    }

    private void addTx(TransactionType type, String from, String to, String amt, int daysAgo, Long by, int hoursAgo) {
        transactionRepository.save(Transaction.builder()
                .type(type).fromIban(from).toIban(to)
                .amount(new BigDecimal(amt)).timestamp(LocalDateTime.now().minusDays(daysAgo).minusHours(hoursAgo))
                .initiatedBy(by).build());
    }
}