package com.sneaky.sneaky.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    @Size(max = 80, message = "Merchant name must be at most 80 characters")
    private String merchantName;

    @Size(max = 1000, message = "Merchant URL must be at most 1000 characters")
    private String merchantUrl;

    private List<@NotBlank(message = "Size cannot be blank") @Size(max = 20, message = "Size must be at most 20 characters") String> sizes;

    @Valid
    private List<ProductColorDTO> colors;

    @Size(max = 40, message = "Stock status must be at most 40 characters")
    private String stockStatus;

    private UUID brandId;

    private Boolean isActive;
}
