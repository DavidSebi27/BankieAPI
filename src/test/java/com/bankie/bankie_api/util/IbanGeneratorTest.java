package com.bankie.bankie_api.util;

import com.bankie.bankie_api.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IbanGeneratorTest {

    @Mock AccountRepository accountRepository;
    @InjectMocks IbanGenerator ibanGenerator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ibanGenerator, "bankCode", "INHO0");
    }

    @Test
    void generate_producesValidDutchIban() {
        when(accountRepository.existsByIban(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        String iban = ibanGenerator.generate();

        assertThat(iban).matches("NL\\d{2}INHO0\\d{9}");
    }

    @Test
    void generate_retriesUntilUnique() {
        when(accountRepository.existsByIban(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true, false);

        String iban = ibanGenerator.generate();

        assertThat(iban).matches("NL\\d{2}INHO0\\d{9}");
        verify(accountRepository, times(2)).existsByIban(org.mockito.ArgumentMatchers.anyString());
    }
}
