package com.sneaky.sneaky.dto.wishlist;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WishlistItemDTO {

    private UUID productId;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String brandName;

}