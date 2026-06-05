package com.bankie.bankie_api.policy;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountPolicyTest {

    private AccountPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AccountPolicy();
    }

    // requireCustomerRole

    @Test
    void requireCustomerRole_passesForCustomer() {
        User u = User.builder().role(Role.CUSTOMER).build();
        assertThatCode(() -> policy.requireCustomerRole(u)).doesNotThrowAnyException();
    }

    @Test
    void requireCustomerRole_throwsForEmployee() {
        User u = User.builder().role(Role.EMPLOYEE).build();
        assertThatThrownBy(() -> policy.requireCustomerRole(u))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only customers");
    }

    // requireNoExistingAccounts

    @Test
    void requireNoExistingAccounts_passesWhenNoneExist() {
        assertThatCode(() -> policy.requireNoExistingAccounts(false)).doesNotThrowAnyException();
    }

    @Test
    void requireNoExistingAccounts_throwsWhenAccountsExist() {
        assertThatThrownBy(() -> policy.requireNoExistingAccounts(true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has accounts");
    }

    // requireValidAbsoluteLimit

    @Test
    void requireValidAbsoluteLimit_passesForNull() {
        assertThatCode(() -> policy.requireValidAbsoluteLimit(null)).doesNotThrowAnyException();
    }

    @Test
    void requireValidAbsoluteLimit_passesForZero() {
        assertThatCode(() -> policy.requireValidAbsoluteLimit(BigDecimal.ZERO)).doesNotThrowAnyException();
    }

    @Test
    void requireValidAbsoluteLimit_passesForNegative() {
        assertThatCode(() -> policy.requireValidAbsoluteLimit(new BigDecimal("-100.00"))).doesNotThrowAnyException();
    }

    @Test
    void requireValidAbsoluteLimit_throwsForPositive() {
        assertThatThrownBy(() -> policy.requireValidAbsoluteLimit(new BigDecimal("50.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be positive");
    }

    // requireValidDailyLimit

    @Test
    void requireValidDailyLimit_passesForNull() {
        assertThatCode(() -> policy.requireValidDailyLimit(null)).doesNotThrowAnyException();
    }

    @Test
    void requireValidDailyLimit_passesForPositive() {
        assertThatCode(() -> policy.requireValidDailyLimit(new BigDecimal("500.00"))).doesNotThrowAnyException();
    }

    @Test
    void requireValidDailyLimit_throwsForNegative() {
        assertThatThrownBy(() -> policy.requireValidDailyLimit(new BigDecimal("-1.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be negative");
    }

    // requireAbsoluteLimitPresent

    @Test
    void requireAbsoluteLimitPresent_passesForNonNull() {
        assertThatCode(() -> policy.requireAbsoluteLimitPresent(BigDecimal.ZERO)).doesNotThrowAnyException();
    }

    @Test
    void requireAbsoluteLimitPresent_throwsForNull() {
        assertThatThrownBy(() -> policy.requireAbsoluteLimitPresent(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Absolute limit is required");
    }

    // requireDailyLimitPresent

    @Test
    void requireDailyLimitPresent_passesForNonNull() {
        assertThatCode(() -> policy.requireDailyLimitPresent(BigDecimal.TEN)).doesNotThrowAnyException();
    }

    @Test
    void requireDailyLimitPresent_throwsForNull() {
        assertThatThrownBy(() -> policy.requireDailyLimitPresent(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Daily transfer limit is required");
    }

    // requireAccountNotClosed

    @Test
    void requireAccountNotClosed_passesForActiveAccount() {
        Account a = Account.builder().status(AccountStatus.ACTIVE).build();
        assertThatCode(() -> policy.requireAccountNotClosed(a)).doesNotThrowAnyException();
    }

    @Test
    void requireAccountNotClosed_throwsForClosedAccount() {
        Account a = Account.builder().status(AccountStatus.CLOSED).build();
        assertThatThrownBy(() -> policy.requireAccountNotClosed(a))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already closed");
    }
}
