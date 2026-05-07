package com.sneaky.sneaky.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CartItemDTO {
    private UUID productId;
    private String name;
    private BigDecimal price;
    private String currency;
    private String imageUrl;
    private String brandName;
    private Integer quantity;
    private BigDecimal itemTotal;
}
