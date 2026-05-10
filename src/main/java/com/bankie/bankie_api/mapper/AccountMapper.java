package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "userId", source = "user.id")
    AccountResponseDTO toResponseDto(Account account);

    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    SearchAccountResponseDTO toSearchResponseDto(Account account);
}
