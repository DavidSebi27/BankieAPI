package com.bankie.bankie_api.service;

import com.bankie.bankie_api.dto.AuthContext;
import com.bankie.bankie_api.dto.request.AccountSearchFilterDTO;
import com.bankie.bankie_api.dto.request.CreateAccountRequestDTO;
import com.bankie.bankie_api.dto.request.SetAbsoluteLimitRequestDTO;
import com.bankie.bankie_api.dto.request.SetDailyLimitRequestDTO;
import com.bankie.bankie_api.dto.request.VerifyRecipientRequestDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.AccountNotFoundException;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.exception.CustomerNotFoundException;
import com.bankie.bankie_api.mapper.AccountMapper;
import com.bankie.bankie_api.policy.AccountPolicy;
import com.bankie.bankie_api.repository.AccountRepository;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.util.IbanGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String EMPLOYEE_EMAIL = "emp@bankie.nl";
    private static final String CUSTOMER_EMAIL = "alice@bankie.nl";
    private static final String IBAN = "NL01INHO0000000001";

    @Mock AccountRepository accountRepository;
    @Mock UserRepository userRepository;
    @Mock AccountMapper accountMapper;
    @Mock IbanGenerator ibanGenerator;
    @Spy AccountPolicy accountPolicy = new AccountPolicy();
    @InjectMocks AccountService service;

    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultDailyLimit", new BigDecimal("1000.00"));
    }

    private AuthContext employeeContext() {
        return new AuthContext(EMPLOYEE_EMAIL, true);
    }

    private AuthContext customerContext() {
        return new AuthContext(CUSTOMER_EMAIL, false);
    }

    @Test
    void getAccountsForUser_withCustomerId_usesFindByUserId() {
        Account account = Account.builder().iban(IBAN).build();
        when(accountRepository.findByUserId(eq(7L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(account)));
        when(accountMapper.toResponseDto(account)).thenReturn(AccountResponseDTO.builder().iban(IBAN).build());

        Page<AccountResponseDTO> result = service.getAccountsForUser(7L, employeeContext(), pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(accountRepository).findByUserId(7L, pageable);
        verify(accountRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAccountsForUser_nullCustomerId_employee_seesAllAccounts() {
        when(accountRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        service.getAccountsForUser(null, employeeContext(), pageable);

        verify(accountRepository).findAll(pageable);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void getAccountsForUser_nullCustomerId_customer_resolvesOwnAccounts() {
        User customer = User.builder().id(5L).email(CUSTOMER_EMAIL).role(Role.CUSTOMER).build();
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(accountRepository.findByUserId(eq(5L), eq(pageable))).thenReturn(Page.empty(pageable));

        service.getAccountsForUser(null, customerContext(), pageable);

        verify(accountRepository).findByUserId(5L, pageable);
        verify(accountRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void searchAccounts_employee_searchesAllOwners() {
        var filter = new AccountSearchFilterDTO("John", "Doe");
        when(accountRepository.searchByOwnerNames("John", "Doe", pageable)).thenReturn(Page.empty(pageable));

        service.searchAccounts(filter, pageable, employeeContext());

        verify(accountRepository).searchByOwnerNames("John", "Doe", pageable);
        verify(accountRepository, never()).searchApprovedByOwnerNames(any(), any(), any());
    }

    @Test
    void searchAccounts_customer_searchesApprovedOwnersOnly() {
        var filter = new AccountSearchFilterDTO("John", "Doe");
        when(accountRepository.searchApprovedByOwnerNames("John", "Doe", pageable)).thenReturn(Page.empty(pageable));

        service.searchAccounts(filter, pageable, customerContext());

        verify(accountRepository).searchApprovedByOwnerNames("John", "Doe", pageable);
        verify(accountRepository, never()).searchByOwnerNames(any(), any(), any());
    }

    @Test
    void verifyRecipient_normalizesIbanAndTrimsNames() {
        Account account = Account.builder().iban(IBAN).build();
        when(accountRepository.findVerifiedRecipient(IBAN, "John", "Doe")).thenReturn(Optional.of(account));
        when(accountMapper.toSearchResponseDto(account))
                .thenReturn(SearchAccountResponseDTO.builder().iban(IBAN).firstName("John").lastName("Doe").build());

        var request = new VerifyRecipientRequestDTO("  nl01 inho 0000000001 ", "  John ", " Doe ");
        SearchAccountResponseDTO result = service.verifyRecipient(request);

        assertThat(result.getIban()).isEqualTo(IBAN);
        verify(accountRepository).findVerifiedRecipient(IBAN, "John", "Doe");
    }

    @Test
    void verifyRecipient_notFound_throws() {
        when(accountRepository.findVerifiedRecipient(eq(IBAN), any(), any())).thenReturn(Optional.empty());

        var request = new VerifyRecipientRequestDTO(IBAN, "John", "Doe");
        assertThatThrownBy(() -> service.verifyRecipient(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(IBAN);
    }

    @Test
    void approveCustomerAndCreateAccounts_approvesAndCreatesCheckingAndSavings() {
        User customer = User.builder().id(3L).role(Role.CUSTOMER).approved(false).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByUser(customer)).thenReturn(false);
        when(ibanGenerator.generate()).thenReturn("NL01INHO000000010", "NL01INHO000000020");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponseDto(any(Account.class))).thenReturn(AccountResponseDTO.builder().build());

        var dto = new CreateAccountRequestDTO(null, null);
        List<AccountResponseDTO> result = service.approveCustomerAndCreateAccounts(3L, dto);

        assertThat(result).hasSize(2);
        assertThat(customer.isApproved()).isTrue();
        verify(userRepository).save(customer);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Account> saved = captor.getAllValues();
        assertThat(saved).extracting(Account::getType)
                .containsExactly(AccountType.CHECKING, AccountType.SAVINGS);
        assertThat(saved.get(0).getAbsoluteLimit()).isEqualByComparingTo("0.00");
        assertThat(saved.get(0).getDailyTransferLimit()).isEqualByComparingTo("1000.00");
        assertThat(saved.get(1).getAbsoluteLimit()).isEqualByComparingTo("0.00");
        assertThat(saved).allSatisfy(a -> {
            assertThat(a.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(a.getBalance()).isEqualByComparingTo("0.00");
            assertThat(a.getUser()).isEqualTo(customer);
        });
    }

    @Test
    void approveCustomerAndCreateAccounts_appliesProvidedLimits() {
        User customer = User.builder().id(3L).role(Role.CUSTOMER).approved(false).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByUser(customer)).thenReturn(false);
        when(ibanGenerator.generate()).thenReturn("NL01INHO000000010", "NL01INHO000000020");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponseDto(any(Account.class))).thenReturn(AccountResponseDTO.builder().build());

        var dto = new CreateAccountRequestDTO(new BigDecimal("-50.00"), new BigDecimal("250.00"));
        service.approveCustomerAndCreateAccounts(3L, dto);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Account checking = captor.getAllValues().get(0);
        assertThat(checking.getAbsoluteLimit()).isEqualByComparingTo("-50.00");
        assertThat(checking.getDailyTransferLimit()).isEqualByComparingTo("250.00");
    }

    @Test
    void approveCustomerAndCreateAccounts_customerNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        var dto = new CreateAccountRequestDTO(null, null);
        assertThatThrownBy(() -> service.approveCustomerAndCreateAccounts(99L, dto))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void approveCustomerAndCreateAccounts_rejectsNonCustomer() {
        User employee = User.builder().id(3L).role(Role.EMPLOYEE).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));

        var dto = new CreateAccountRequestDTO(null, null);
        assertThatThrownBy(() -> service.approveCustomerAndCreateAccounts(3L, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("customers");

        verify(accountRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void approveCustomerAndCreateAccounts_rejectsWhenCustomerAlreadyHasAccounts() {
        User customer = User.builder().id(3L).role(Role.CUSTOMER).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByUser(customer)).thenReturn(true);

        var dto = new CreateAccountRequestDTO(null, null);
        assertThatThrownBy(() -> service.approveCustomerAndCreateAccounts(3L, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has accounts");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void approveCustomerAndCreateAccounts_rejectsPositiveAbsoluteLimit() {
        User customer = User.builder().id(3L).role(Role.CUSTOMER).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByUser(customer)).thenReturn(false);

        var dto = new CreateAccountRequestDTO(new BigDecimal("10.00"), null);
        assertThatThrownBy(() -> service.approveCustomerAndCreateAccounts(3L, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Absolute limit");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void closeAccount_setsStatusClosed() {
        Account account = Account.builder().iban(IBAN).status(AccountStatus.ACTIVE).build();
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponseDto(account)).thenReturn(AccountResponseDTO.builder().iban(IBAN).build());

        service.closeAccount(IBAN);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(accountRepository).save(account);
    }

    @Test
    void closeAccount_missing_throws() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.closeAccount(IBAN))
                .isInstanceOf(AccountNotFoundException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void closeAccount_rejectsAlreadyClosed() {
        Account account = Account.builder().iban(IBAN).status(AccountStatus.CLOSED).build();
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.closeAccount(IBAN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already closed");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateAbsoluteLimit_updatesField() {
        Account account = Account.builder().iban(IBAN).build();
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponseDto(account)).thenReturn(AccountResponseDTO.builder().build());

        var dto = new SetAbsoluteLimitRequestDTO();
        dto.setAbsoluteLimit(new BigDecimal("-100.00"));
        service.updateAbsoluteLimit(IBAN, dto);

        assertThat(account.getAbsoluteLimit()).isEqualByComparingTo("-100.00");
        verify(accountRepository).save(account);
    }

    @Test
    void updateAbsoluteLimit_rejectsMissingValue() {
        var dto = new SetAbsoluteLimitRequestDTO();
        assertThatThrownBy(() -> service.updateAbsoluteLimit(IBAN, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("required");
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void updateAbsoluteLimit_missingAccount_throws() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.empty());

        var dto = new SetAbsoluteLimitRequestDTO();
        dto.setAbsoluteLimit(new BigDecimal("-100.00"));
        assertThatThrownBy(() -> service.updateAbsoluteLimit(IBAN, dto))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void updateDailyTransferLimit_updatesField() {
        Account account = Account.builder().iban(IBAN).build();
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponseDto(account)).thenReturn(AccountResponseDTO.builder().build());

        var dto = new SetDailyLimitRequestDTO();
        dto.setDailyTransferLimit(new BigDecimal("500.00"));
        service.updateDailyTransferLimit(IBAN, dto);

        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("500.00");
        verify(accountRepository).save(account);
    }

    @Test
    void updateDailyTransferLimit_rejectsMissingValue() {
        var dto = new SetDailyLimitRequestDTO();
        assertThatThrownBy(() -> service.updateDailyTransferLimit(IBAN, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("required");
        verify(accountRepository, never()).findById(any());
    }
}
