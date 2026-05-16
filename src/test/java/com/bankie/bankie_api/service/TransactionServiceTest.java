package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.request.AtmRequestDTO;
import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.mapper.TransactionMapper;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.TransactionRepository;
import com.bankie.bankie_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String FROM = "NL01BANK0000000001";
    private static final String TO   = "NL01BANK0000000002";
    private static final String EMPLOYEE_EMAIL = "emp@bankie.nl";
    private static final String CUSTOMER_EMAIL = "alice@bankie.nl";
    private static final String CUSTOMER_IBAN  = "NL01INHO0000000001";

    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock TransactionMapper transactionMapper;
    @InjectMocks TransactionService service;

    private User customer;
    private User employee;
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(10L).firstName("Cus").lastName("Tomer").role(Role.CUSTOMER).build();
        employee = User.builder().id(99L).firstName("Em").lastName("Ployee").email(EMPLOYEE_EMAIL).role(Role.EMPLOYEE).build();

        source = Account.builder()
                .iban(FROM).type(AccountType.CHECKING).status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("500.00")).currency("EUR")
                .absoluteLimit(new BigDecimal("0.00"))
                .dailyTransferLimit(new BigDecimal("1000.00"))
                .user(customer).build();

        destination = Account.builder()
                .iban(TO).type(AccountType.CHECKING).status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("100.00")).currency("EUR")
                .absoluteLimit(new BigDecimal("0.00"))
                .dailyTransferLimit(new BigDecimal("1000.00"))
                .user(customer).build();
    }

    private TransferRequestDTO request(BigDecimal amount) {
        return new TransferRequestDTO(FROM, TO, amount);
    }

    private AtmRequestDTO atm(String iban, BigDecimal amount) {
        return new AtmRequestDTO(iban, amount);
    }

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
    void transfer_happyPath_debitsSourceCreditsDestinationAndPersistsTransaction() {
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(transactionRepository.sumDailyMovementsSince(eq(FROM), any()))
                .thenReturn(BigDecimal.ZERO);
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(transactionMapper.toResponseDto(any(Transaction.class)))
                .thenReturn(TransactionResponseDTO.builder().id(1L).type(TransactionType.TRANSFER).build());

        TransactionResponseDTO result = service.transfer(request(new BigDecimal("150.00")), EMPLOYEE_EMAIL);

        assertThat(source.getBalance()).isEqualByComparingTo("350.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("250.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(saved.getFromIban()).isEqualTo(FROM);
        assertThat(saved.getToIban()).isEqualTo(TO);
        assertThat(saved.getAmount()).isEqualByComparingTo("150.00");
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getInitiatedBy()).isEqualTo(99L);
        assertThat(saved.getTimestamp()).isNotNull();

        assertThat(result.getInitiatedByName()).isEqualTo("Em Ployee");
    }

    @Test
    void transfer_rejectsWhenSourceEqualsDestination() {
        TransferRequestDTO sameAccount = new TransferRequestDTO(FROM, FROM, new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.transfer(sameAccount, EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsWhenSourceAccountMissing() {
        when(accountRepository.findById(FROM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("10.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Source account not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsWhenDestinationIsSavingsForExternalTransfer() {
        destination.setType(AccountType.SAVINGS);
        User otherCustomer = User.builder().id(11L).firstName("Other").lastName("Cust").role(Role.CUSTOMER).build();
        destination.setUser(otherCustomer);
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("10.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("checking");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsWhenSourceNotActive() {
        source.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("10.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsWhenAccountOwnerIsNotCustomer() {
        source.setUser(employee);
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("10.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("customer");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsCurrencyMismatch() {
        destination.setCurrency("USD");
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("10.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("currency");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsWhenBalanceWouldBreachAbsoluteLimit() {
        source.setBalance(new BigDecimal("50.00"));
        source.setAbsoluteLimit(new BigDecimal("0.00"));
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("100.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("absolute limit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsWhenAmountExceedsDailyTransferLimit() {
        source.setDailyTransferLimit(new BigDecimal("200.00"));
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));
        when(transactionRepository.sumDailyMovementsSince(eq(FROM), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("150.00"));

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("100.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("daily transfer limit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_customerCannotTransferFromAccountTheyDoNotOwn() {
        User otherCustomer = User.builder().id(11L).email("other@bankie.nl").firstName("Other").lastName("One").role(Role.CUSTOMER).build();
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(accountRepository.findById(TO)).thenReturn(Optional.of(destination));
        when(userRepository.findByEmail("other@bankie.nl")).thenReturn(Optional.of(otherCustomer));

        assertThatThrownBy(() -> service.transfer(request(new BigDecimal("10.00")), "other@bankie.nl"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not own");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_happyPath_debitsBalanceAndPersistsWithdrawal() {
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(transactionRepository.sumDailyMovementsSince(eq(FROM), any())).thenReturn(BigDecimal.ZERO);
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(transactionMapper.toResponseDto(any(Transaction.class)))
                .thenReturn(TransactionResponseDTO.builder().id(1L).type(TransactionType.WITHDRAWAL).build());

        service.withdraw(atm(FROM, new BigDecimal("100.00")), EMPLOYEE_EMAIL);

        assertThat(source.getBalance()).isEqualByComparingTo("400.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(saved.getFromIban()).isEqualTo(FROM);
        assertThat(saved.getToIban()).isNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void withdraw_rejectsWhenBalanceWouldBreachAbsoluteLimit() {
        source.setBalance(new BigDecimal("50.00"));
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.withdraw(atm(FROM, new BigDecimal("100.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("absolute limit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_rejectsWhenAmountExceedsDailyLimit() {
        source.setDailyTransferLimit(new BigDecimal("200.00"));
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(transactionRepository.sumDailyMovementsSince(eq(FROM), any()))
                .thenReturn(new BigDecimal("150.00"));

        assertThatThrownBy(() -> service.withdraw(atm(FROM, new BigDecimal("100.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("daily transfer limit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_rejectsWhenAccountMissing() {
        when(accountRepository.findById(FROM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(atm(FROM, new BigDecimal("10.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void deposit_happyPath_creditsBalanceAndPersistsDeposit() {
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(transactionRepository.sumDailyMovementsSince(eq(FROM), any())).thenReturn(BigDecimal.ZERO);
        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });
        when(transactionMapper.toResponseDto(any(Transaction.class)))
                .thenReturn(TransactionResponseDTO.builder().id(2L).type(TransactionType.DEPOSIT).build());

        service.deposit(atm(FROM, new BigDecimal("75.00")), EMPLOYEE_EMAIL);

        assertThat(source.getBalance()).isEqualByComparingTo("575.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getFromIban()).isNull();
        assertThat(saved.getToIban()).isEqualTo(FROM);
        assertThat(saved.getAmount()).isEqualByComparingTo("75.00");
    }

    @Test
    void deposit_rejectsWhenAmountExceedsDailyLimit() {
        source.setDailyTransferLimit(new BigDecimal("200.00"));
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));
        when(transactionRepository.sumDailyMovementsSince(eq(FROM), any()))
                .thenReturn(new BigDecimal("150.00"));

        assertThatThrownBy(() -> service.deposit(atm(FROM, new BigDecimal("100.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("daily transfer limit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deposit_rejectsWhenAccountIsSavings() {
        source.setType(AccountType.SAVINGS);
        when(accountRepository.findById(FROM)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.deposit(atm(FROM, new BigDecimal("50.00")), EMPLOYEE_EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("checking");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void findAll_customer_seesTransactionsOnBothSidesOfOwnIbans() {
        Pageable pageable = PageRequest.of(0, 20);
        User c = customerUser();
        Account account = customerAccount();

        Transaction tx = Transaction.builder()
                .id(1L)
                .toIban(CUSTOMER_IBAN)
                .fromIban("NL99EXTERNAL000000")
                .type(TransactionType.TRANSFER)
                .amount(BigDecimal.TEN)
                .build();

        TransactionResponseDTO dto = TransactionResponseDTO.builder().id(1L).build();

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(c));
        when(accountRepository.findByUser(c)).thenReturn(List.of(account));
        when(transactionRepository.findAllFiltered(
                isNull(), eq(List.of(CUSTOMER_IBAN)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(transactionMapper.toResponseDto(eq(tx), any(), any())).thenReturn(dto);

        Page<TransactionResponseDTO> result = service.findAll(
                null, null, null, null, null, null, null, pageable, CUSTOMER_EMAIL, false);

        assertThat(result.getContent()).containsExactly(dto);
        verify(transactionRepository).findAllFiltered(
                isNull(), eq(List.of(CUSTOMER_IBAN)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void findAll_customer_returnsEmptyPageWhenIbanFilterIsNotOwned() {
        Pageable pageable = PageRequest.of(0, 20);
        User c = customerUser();
        Account account = customerAccount();

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(c));
        when(accountRepository.findByUser(c)).thenReturn(List.of(account));

        Page<TransactionResponseDTO> result = service.findAll(
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

        Page<TransactionResponseDTO> result = service.findAll(
                targetUserId, null, null, null, null, null, null, pageable, "employee@bankie.nl", true);

        assertThat(result.isEmpty()).isTrue();
        verify(transactionRepository).findAllFiltered(
                eq(targetUserId), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(accountRepository);
    }
}