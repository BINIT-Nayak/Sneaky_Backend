package com.sneaky.sneaky.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductDTO {
    private UUID id;
    private String name;
    private BigDecimal price;
    private String image;
    private String description;
    private String brand;
    private String category;
}
