package com.bankie.bankie_api.policy;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class TransactionPolicy {

    public void requireDifferentAccounts(String fromIban, String toIban) {
        if (Objects.equals(fromIban, toIban)) {
            throw new BusinessRuleException("Source and destination accounts must be different");
        }
    }

    public void requireActiveCustomerChecking(Account account, String label) {
        if (account.getType() != AccountType.CHECKING) {
            throw new BusinessRuleException(label + " account must be a checking account");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(label + " account is not active");
        }
        if (account.getUser() == null || account.getUser().getRole() != Role.CUSTOMER) {
            throw new BusinessRuleException(label + " account must belong to a customer");
        }
    }

    public void requireSameCurrency(Account source, Account destination) {
        if (!Objects.equals(source.getCurrency(), destination.getCurrency())) {
            throw new BusinessRuleException("Accounts must share the same currency");
        }
    }

    public void requireWithinAbsoluteLimit(Account account, BigDecimal newBalance, String label) {
        if (newBalance.compareTo(account.getAbsoluteLimit()) < 0) {
            throw new BusinessRuleException(label + " would breach the account's absolute limit");
        }
    }

    public void requireWithinDailyLimit(Account account, BigDecimal amount, BigDecimal usedToday, String label) {
        if (usedToday.add(amount).compareTo(account.getDailyTransferLimit()) > 0) {
            throw new BusinessRuleException(label + " exceeds the account's daily transfer limit");
        }
    }
}
