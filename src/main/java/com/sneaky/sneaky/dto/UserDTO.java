package com.sneaky.sneaky.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDTO {
    private UUID userId;
    private String name;
    private String email;
    private Boolean isGuest;
}
