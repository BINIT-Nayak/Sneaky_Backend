package com.sneaky.sneaky.dto.wishlist;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToWishlistRequestDTO {
    @NotNull(message = "Product ID is required")
    private UUID productId;
}
