package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.request.RegisterRequest;
import com.bankie.bankie_api.dto.response.LoginResponse;
import com.bankie.bankie_api.dto.response.UserResponseDTO;
import com.bankie.bankie_api.dto.response.UserSummary;
import com.bankie.bankie_api.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "CUSTOMER")
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "firstName", source = "request.firstName")
    @Mapping(target = "lastName", source = "request.lastName")
    @Mapping(target = "email", source = "request.email")
    @Mapping(target = "bsn", source = "request.bsn")
    @Mapping(target = "phoneNumber", source = "request.phoneNumber")
    User toEntity(RegisterRequest request, String encodedPassword);

    UserResponseDTO toResponseDto(User user);

    UserSummary toSummary(User user);

    @Mapping(target = "token", source = "token")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "approved", source = "user.approved")
    LoginResponse toLoginResponse(User user, String token);
}
