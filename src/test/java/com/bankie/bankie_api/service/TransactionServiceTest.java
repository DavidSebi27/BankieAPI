package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.mapper.TransactionMapper;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock TransactionMapper transactionMapper;
    @InjectMocks TransactionService transactionService;

    private static final String CUSTOMER_EMAIL = "alice@bankie.nl";
    private static final String CUSTOMER_IBAN  = "NL01INHO0000000001";

    private User customerUser() {
        return User.builder().id(1L).email(CUSTOMER_EMAIL).role(Role.CUSTOMER).approved(true).build();
    }

    private Account customerAccount() {
        Account a = new Account();
        a.setIban(CUSTOMER_IBAN);
        a.setUser(customerUser());
        return a;
    }

    @Test
    void findAll_customer_seesTransactionsOnBothSidesOfOwnIbans() {
        Pageable pageable = PageRequest.of(0, 20);
        User customer = customerUser();
        Account account = customerAccount();

        Transaction tx = Transaction.builder()
                .id(1L)
                .toIban(CUSTOMER_IBAN)
                .fromIban("NL99EXTERNAL000000")
                .type(TransactionType.TRANSFER)
                .amount(BigDecimal.TEN)
                .build();

        TransactionResponseDTO dto = TransactionResponseDTO.builder().id(1L).build();

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(accountRepository.findByUser(customer)).thenReturn(List.of(account));
        when(transactionRepository.findAllFiltered(
                isNull(), eq(List.of(CUSTOMER_IBAN)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(transactionMapper.toResponseDto(eq(tx), any(), any())).thenReturn(dto);

        Page<TransactionResponseDTO> result = transactionService.findAll(
                null, null, null, null, null, null, null, pageable, CUSTOMER_EMAIL, false);

        assertThat(result.getContent()).containsExactly(dto);
        verify(transactionRepository).findAllFiltered(
                isNull(), eq(List.of(CUSTOMER_IBAN)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void findAll_customer_returnsEmptyPageWhenIbanFilterIsNotOwned() {
        Pageable pageable = PageRequest.of(0, 20);
        User customer = customerUser();
        Account account = customerAccount();

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(accountRepository.findByUser(customer)).thenReturn(List.of(account));

        Page<TransactionResponseDTO> result = transactionService.findAll(
                null, null, "NL99SOMEONEELSE0000", null, null, null, null, pageable, CUSTOMER_EMAIL, false);

        assertThat(result.isEmpty()).isTrue();
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void findAll_employee_canFilterByAnyInitiatedBy() {
        Pageable pageable = PageRequest.of(0, 20);
        Long targetUserId = 42L;

        when(transactionRepository.findAllFiltered(
                eq(targetUserId), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(Page.empty());

        Page<TransactionResponseDTO> result = transactionService.findAll(
                targetUserId, null, null, null, null, null, null, pageable, "employee@bankie.nl", true);

        assertThat(result.isEmpty()).isTrue();
        verify(transactionRepository).findAllFiltered(
                eq(targetUserId), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(accountRepository);
    }
}
