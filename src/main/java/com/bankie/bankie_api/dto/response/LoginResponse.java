package com.bankie.bankie_api.dto.response;

import com.bankie.bankie_api.enums.Role;

public record LoginResponse(String token, Role role, boolean approved) {}