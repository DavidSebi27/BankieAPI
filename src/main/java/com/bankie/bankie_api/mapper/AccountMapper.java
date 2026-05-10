package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.AccountResponseDTO;
import com.bankie.bankie_api.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    public AccountResponseDTO toResponseDto(Account account)
    {
        if (account == null) return null;

        return AccountResponseDTO.builder()
                .iban(account.getIban())
                .type(account.getType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .absoluteLimit(account.getAbsoluteLimit())
                .dailyTransferLimit(account.getDailyTransferLimit())
                .userId(account.getUser() != null ? account.getUser().getId() : null)
                .build();
    }
}
