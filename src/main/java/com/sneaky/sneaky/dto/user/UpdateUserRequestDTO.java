package com.sneaky.sneaky.dto.user;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateUserRequestDTO {
    private String name;
    private String email;
    private String password;
    private Boolean isGuest;

}
