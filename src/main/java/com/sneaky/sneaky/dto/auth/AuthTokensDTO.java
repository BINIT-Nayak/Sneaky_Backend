package com.sneaky.sneaky.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokensDTO {
    private String accessToken;
    private String refreshToken;

    public LoginResponseDTO toLoginResponse() {
        return new LoginResponseDTO(accessToken);
    }
}
