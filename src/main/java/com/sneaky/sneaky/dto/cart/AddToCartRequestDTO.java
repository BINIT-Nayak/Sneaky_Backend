package com.sneaky.sneaky.dto.cart;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequestDTO {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;
}
