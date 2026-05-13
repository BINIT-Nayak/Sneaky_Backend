package com.sneaky.sneaky.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductColorDTO {

    @NotBlank(message = "Color name is required")
    @Size(max = 40, message = "Color name must be at most 40 characters")
    private String name;

    @NotBlank(message = "Color value is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color value must be a hex color")
    private String value;
}
