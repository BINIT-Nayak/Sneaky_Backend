package com.sneaky.sneaky.dto.wishlist;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToWishlistRequestDTO {
    private UUID productId;
}