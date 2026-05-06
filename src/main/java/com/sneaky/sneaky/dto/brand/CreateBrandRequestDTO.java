package com.sneaky.sneaky.dto.brand;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBrandRequestDTO {

    @NotBlank(message = "Brand name is required")
    private String name;
}
