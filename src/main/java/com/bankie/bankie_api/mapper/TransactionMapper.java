package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "initiatedByName", ignore = true)
    TransactionResponseDTO toResponseDto(Transaction transaction);
}
