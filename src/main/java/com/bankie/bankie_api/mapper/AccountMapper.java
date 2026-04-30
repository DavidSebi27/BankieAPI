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
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .absoluteLimit(account.getAbsoluteLimit())
                .dailyTransferLimit(account.getDailyTransferLimit())
                .ownerId(account.getOwner() != null ? account.getOwner().getId() : null)
                .build();
    }
}
