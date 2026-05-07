package com.sneaky.sneaky.dto.cart;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequestDTO {
    private UUID productId;
    private Integer quantity;
}
