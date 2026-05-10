package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchAccountResponseDTO {
    private String firstName;
    private String lastName;
    private String iban;
}
