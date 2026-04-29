package com.bankie.bankie_api.mapper;

import com.bankie.bankie_api.dto.UserRequestDTO;
import com.bankie.bankie_api.dto.UserResponseDTO;
import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(UserRequestDTO dto) {
        if (dto == null) return null;

        return User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .bsn(dto.getBsn())
                .phoneNumber(dto.getPhoneNumber())
                .role(Role.CUSTOMER)
                .approved(false)
                .build();
    }

    public UserResponseDTO toResponseDto(User user)
    {
        if (user == null) return null;

        return UserResponseDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .bsn(user.getBsn())
                .role(user.getRole())
                .approved(user.isApproved())
                .build();
    }

}
