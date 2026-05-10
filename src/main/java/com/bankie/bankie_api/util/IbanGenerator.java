package com.bankie.bankie_api.util;

import com.bankie.bankie_api.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class IbanGenerator {

    @Value("${bankie.iban.bank-code}")
    private String bankCode;

    private final AccountRepository accountRepository;
    private final Random random = new Random();

    public String generate() {
        String iban;
        do {
            iban = "NL" + twoDigits() + bankCode + nineDigits();
        } while (accountRepository.existsByIban(iban));
        return iban;
    }

    private String twoDigits() {
        return String.format("%02d", random.nextInt(100));
    }

    private String nineDigits() {
        return String.format("%09d", random.nextInt(1_000_000_000));
    }
}