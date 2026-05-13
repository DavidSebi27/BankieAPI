package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    @Mapping(target = "initiatedByName", ignore = true)
    TransactionResponseDTO toResponseDto(Transaction transaction);

    @Mapping(target = "fromName", source = "fromName")
    @Mapping(target = "toName", source = "toName")
    @Mapping(target = "initiatedBy", source = "transaction.initiatedBy")
    TransactionResponseDTO toResponseDto(Transaction transaction, String fromName, String toName);
}