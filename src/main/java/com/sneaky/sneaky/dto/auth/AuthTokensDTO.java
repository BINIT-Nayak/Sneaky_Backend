package com.sneaky.sneaky.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokensDTO {
    private String accessToken;
    private String refreshToken;
    private String role;

    public AuthTokensDTO(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "USER");
    }

    public LoginResponseDTO toLoginResponse() {
        return new LoginResponseDTO(accessToken, role);
    }
}
