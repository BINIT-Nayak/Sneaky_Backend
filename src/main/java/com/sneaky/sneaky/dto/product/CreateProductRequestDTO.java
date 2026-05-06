package com.sneaky.sneaky.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductRequestDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private UUID brandId;
    private Boolean isActive = true;
}
