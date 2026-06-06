package com.bankie.bankie_api.policy;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionPolicyTest {

    private TransactionPolicy policy;
    private Account activeChecking;
    private User customer;

    @BeforeEach
    void setUp() {
        policy = new TransactionPolicy();
        customer = User.builder().id(1L).role(Role.CUSTOMER).build();
        activeChecking = Account.builder()
                .iban("NL01BANK0000000001")
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .currency("EUR")
                .balance(new BigDecimal("500.00"))
                .absoluteLimit(new BigDecimal("0.00"))
                .dailyTransferLimit(new BigDecimal("1000.00"))
                .user(customer)
                .build();
    }

    // requireDifferentAccounts

    @Test
    void requireDifferentAccounts_allowsDistinctIbans() {
        assertThatCode(() -> policy.requireDifferentAccounts("NL01A", "NL02B"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireDifferentAccounts_throwsWhenEqual() {
        assertThatThrownBy(() -> policy.requireDifferentAccounts("NL01A", "NL01A"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different");
    }

    // requireActiveCustomerChecking

    @Test
    void requireActiveCustomerChecking_passesForActiveCustomerCheckingAccount() {
        assertThatCode(() -> policy.requireActiveCustomerChecking(activeChecking, "Source"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireActiveCustomerChecking_throwsForSavingsAccount() {
        activeChecking.setType(AccountType.SAVINGS);
        assertThatThrownBy(() -> policy.requireActiveCustomerChecking(activeChecking, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("checking");
    }

    @Test
    void requireActiveCustomerChecking_throwsForClosedAccount() {
        activeChecking.setStatus(AccountStatus.CLOSED);
        assertThatThrownBy(() -> policy.requireActiveCustomerChecking(activeChecking, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void requireActiveCustomerChecking_throwsWhenOwnerIsEmployee() {
        activeChecking.setUser(User.builder().role(Role.EMPLOYEE).build());
        assertThatThrownBy(() -> policy.requireActiveCustomerChecking(activeChecking, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("customer");
    }

    @Test
    void requireActiveCustomerChecking_throwsWhenOwnerIsNull() {
        activeChecking.setUser(null);
        assertThatThrownBy(() -> policy.requireActiveCustomerChecking(activeChecking, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("customer");
    }

    @Test
    void requireActiveCustomerChecking_includesLabelInMessage() {
        activeChecking.setType(AccountType.SAVINGS);
        assertThatThrownBy(() -> policy.requireActiveCustomerChecking(activeChecking, "Destination"))
                .hasMessageStartingWith("Destination");
    }

    // requireActiveCustomerOwned

    @Test
    void requireActiveCustomerOwned_passesForActiveCustomerSavingsAccount() {
        activeChecking.setType(AccountType.SAVINGS);
        assertThatCode(() -> policy.requireActiveCustomerOwned(activeChecking, "Source"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireActiveCustomerOwned_throwsForClosedAccount() {
        activeChecking.setStatus(AccountStatus.CLOSED);
        assertThatThrownBy(() -> policy.requireActiveCustomerOwned(activeChecking, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void requireActiveCustomerOwned_throwsWhenOwnerIsEmployee() {
        activeChecking.setUser(User.builder().role(Role.EMPLOYEE).build());
        assertThatThrownBy(() -> policy.requireActiveCustomerOwned(activeChecking, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("customer");
    }

    // requireOwnership

    @Test
    void requireOwnership_passesWhenUserOwnsAccount() {
        assertThatCode(() -> policy.requireOwnership(activeChecking, customer, "Source"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireOwnership_throwsWhenUserDoesNotOwnAccount() {
        User other = User.builder().id(2L).role(Role.CUSTOMER).build();
        assertThatThrownBy(() -> policy.requireOwnership(activeChecking, other, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not own");
    }

    @Test
    void requireOwnership_throwsWhenAccountHasNoOwner() {
        activeChecking.setUser(null);
        assertThatThrownBy(() -> policy.requireOwnership(activeChecking, customer, "Source"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not own");
    }

    // requireExternalTransferShape

    @Test
    void requireExternalTransferShape_passesForInternalTransferToSavings() {
        Account savings = Account.builder()
                .type(AccountType.SAVINGS).status(AccountStatus.ACTIVE)
                .currency("EUR").user(customer).build();
        assertThatCode(() -> policy.requireExternalTransferShape(activeChecking, savings))
                .doesNotThrowAnyException();
    }

    @Test
    void requireExternalTransferShape_passesForExternalCheckingToChecking() {
        User otherCustomer = User.builder().id(2L).role(Role.CUSTOMER).build();
        Account otherChecking = Account.builder()
                .type(AccountType.CHECKING).status(AccountStatus.ACTIVE)
                .currency("EUR").user(otherCustomer).build();
        assertThatCode(() -> policy.requireExternalTransferShape(activeChecking, otherChecking))
                .doesNotThrowAnyException();
    }

    @Test
    void requireExternalTransferShape_throwsWhenExternalSourceIsSavings() {
        activeChecking.setType(AccountType.SAVINGS);
        User otherCustomer = User.builder().id(2L).role(Role.CUSTOMER).build();
        Account otherChecking = Account.builder()
                .type(AccountType.CHECKING).status(AccountStatus.ACTIVE)
                .currency("EUR").user(otherCustomer).build();
        assertThatThrownBy(() -> policy.requireExternalTransferShape(activeChecking, otherChecking))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("checking");
    }

    @Test
    void requireExternalTransferShape_throwsWhenExternalDestinationIsSavings() {
        User otherCustomer = User.builder().id(2L).role(Role.CUSTOMER).build();
        Account otherSavings = Account.builder()
                .type(AccountType.SAVINGS).status(AccountStatus.ACTIVE)
                .currency("EUR").user(otherCustomer).build();
        assertThatThrownBy(() -> policy.requireExternalTransferShape(activeChecking, otherSavings))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("checking");
    }

    // requireSameCurrency

    @Test
    void requireSameCurrency_passesForMatchingCurrencies() {
        Account other = Account.builder().currency("EUR").build();
        assertThatCode(() -> policy.requireSameCurrency(activeChecking, other))
                .doesNotThrowAnyException();
    }

    @Test
    void requireSameCurrency_throwsForMismatch() {
        Account other = Account.builder().currency("USD").build();
        assertThatThrownBy(() -> policy.requireSameCurrency(activeChecking, other))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("currency");
    }

    // requireWithinAbsoluteLimit

    @Test
    void requireWithinAbsoluteLimit_passesWhenBalanceEqualsLimit() {
        assertThatCode(() -> policy.requireWithinAbsoluteLimit(activeChecking, new BigDecimal("0.00"), "Transfer"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireWithinAbsoluteLimit_passesWhenBalanceAboveLimit() {
        assertThatCode(() -> policy.requireWithinAbsoluteLimit(activeChecking, new BigDecimal("100.00"), "Transfer"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireWithinAbsoluteLimit_throwsWhenBalanceBelowLimit() {
        activeChecking.setAbsoluteLimit(new BigDecimal("-100.00"));
        assertThatThrownBy(() -> policy.requireWithinAbsoluteLimit(activeChecking, new BigDecimal("-200.00"), "Transfer"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("absolute limit");
    }

    // requireWithinDailyLimit

    @Test
    void requireWithinDailyLimit_passesWhenSumEqualsLimit() {
        assertThatCode(() -> policy.requireWithinDailyLimit(
                activeChecking, new BigDecimal("400.00"), new BigDecimal("600.00"), "Transfer"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireWithinDailyLimit_passesWhenSumBelowLimit() {
        assertThatCode(() -> policy.requireWithinDailyLimit(
                activeChecking, new BigDecimal("100.00"), new BigDecimal("100.00"), "Transfer"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireWithinDailyLimit_throwsWhenSumExceedsLimit() {
        assertThatThrownBy(() -> policy.requireWithinDailyLimit(
                activeChecking, new BigDecimal("500.00"), new BigDecimal("600.00"), "Transfer"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("daily transfer limit");
    }
}
