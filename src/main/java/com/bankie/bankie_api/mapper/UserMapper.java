package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.request.UserRequestDTO;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "CUSTOMER")
    @Mapping(target = "approved", constant = "false")
    User toEntity(UserRequestDTO dto);

    UserResponseDTO toResponseDto(User user);
}
