package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.request.AtmRequestDTO;
import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    @Mapping(target = "initiatedByName", ignore = true)
    @Mapping(target = "fromName", source = "fromAccount", qualifiedByName = "accountToName")
    @Mapping(target = "toName", source = "toAccount", qualifiedByName = "accountToName")
    TransactionResponseDTO toResponseDto(Transaction transaction);

    @Mapping(target = "id", source = "transaction.id")
    @Mapping(target = "type", source = "transaction.type")
    @Mapping(target = "fromIban", source = "transaction.fromIban")
    @Mapping(target = "toIban", source = "transaction.toIban")
    @Mapping(target = "amount", source = "transaction.amount")
    @Mapping(target = "currency", source = "transaction.currency")
    @Mapping(target = "timestamp", source = "transaction.timestamp")
    @Mapping(target = "initiatedBy", source = "transaction.initiatedBy")
    @Mapping(target = "fromName", source = "transaction.fromAccount", qualifiedByName = "accountToName")
    @Mapping(target = "toName", source = "transaction.toAccount", qualifiedByName = "accountToName")
    @Mapping(target = "initiatedByName", expression = "java(initiator.getFirstName() + \" \" + initiator.getLastName())")
    TransactionResponseDTO toResponseDto(Transaction transaction, User initiator);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fromAccount", ignore = true)
    @Mapping(target = "toAccount", ignore = true)
    @Mapping(target = "type", constant = "TRANSFER")
    @Mapping(target = "fromIban", source = "request.fromIban")
    @Mapping(target = "toIban", source = "request.toIban")
    @Mapping(target = "amount", source = "request.amount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "initiatedBy", source = "initiatedBy")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    Transaction toTransferEntity(TransferRequestDTO request, Long initiatedBy, String currency);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fromAccount", ignore = true)
    @Mapping(target = "toAccount", ignore = true)
    @Mapping(target = "type", constant = "WITHDRAWAL")
    @Mapping(target = "fromIban", source = "request.iban")
    @Mapping(target = "toIban", ignore = true)
    @Mapping(target = "amount", source = "request.amount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "initiatedBy", source = "initiatedBy")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    Transaction toWithdrawalEntity(AtmRequestDTO request, Long initiatedBy, String currency);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fromAccount", ignore = true)
    @Mapping(target = "toAccount", ignore = true)
    @Mapping(target = "type", constant = "DEPOSIT")
    @Mapping(target = "fromIban", ignore = true)
    @Mapping(target = "toIban", source = "request.iban")
    @Mapping(target = "amount", source = "request.amount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "initiatedBy", source = "initiatedBy")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    Transaction toDepositEntity(AtmRequestDTO request, Long initiatedBy, String currency);

    @Named("accountToName")
    default String accountToName(Account account) {
        if (account == null || account.getUser() == null) return "External/ATM";
        return account.getUser().getFirstName() + " " + account.getUser().getLastName();
    }
}
