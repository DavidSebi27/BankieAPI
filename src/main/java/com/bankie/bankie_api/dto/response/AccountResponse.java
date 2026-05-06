package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;

import java.math.BigDecimal;

public record AccountResponse(
        String iban,
        AccountType type,
        BigDecimal balance,
        AccountStatus status,
        BigDecimal absoluteLimit,
        BigDecimal dailyTransferLimit
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getIban(),
                a.getType(),
                a.getBalance(),
                a.getStatus(),
                a.getAbsoluteLimit(),
                a.getDailyTransferLimit()
        );
    }
}