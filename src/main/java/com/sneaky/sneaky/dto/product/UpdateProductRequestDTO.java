package com.sneaky.sneaky.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductRequestDTO {
    @Size(min = 1, max = 120, message = "Product name must be between 1 and 120 characters")
    private String name;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Size(max = 1000, message = "Image URL must be at most 1000 characters")
    private String imageUrl;

    @Size(max = 80, message = "Category must be at most 80 characters")
    private String category;

    private UUID brandId;

    private Boolean isActive;
}
