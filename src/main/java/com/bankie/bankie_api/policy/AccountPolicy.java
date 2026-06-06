package com.bankie.bankie_api.policy;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountPolicy {

    public void requireCustomerRole(User user) {
        if (user.getRole() != Role.CUSTOMER) {
            throw new BusinessRuleException("Only customers can be approved");
        }
    }

    public void requireNoExistingAccounts(boolean hasAccounts) {
        if (hasAccounts) {
            throw new BusinessRuleException("Customer already has accounts");
        }
    }

    public void requireValidAbsoluteLimit(BigDecimal absoluteLimit) {
        if (absoluteLimit != null && absoluteLimit.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Absolute limit cannot be positive");
        }
    }

    public void requireValidDailyLimit(BigDecimal dailyLimit) {
        if (dailyLimit != null && dailyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Daily transfer limit cannot be negative");
        }
    }

    public void requireAbsoluteLimitPresent(BigDecimal absoluteLimit) {
        if (absoluteLimit == null) {
            throw new BusinessRuleException("Absolute limit is required");
        }
    }

    public void requireDailyLimitPresent(BigDecimal dailyLimit) {
        if (dailyLimit == null) {
            throw new BusinessRuleException("Daily transfer limit is required");
        }
    }

    public void requireAccountNotClosed(Account account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Account is already closed");
        }
    }
}
