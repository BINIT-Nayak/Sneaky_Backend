package com.sneaky.sneaky.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequestDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}