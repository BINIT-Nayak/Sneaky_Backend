package com.sneaky.sneaky.dto.wishlist;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sneaky.sneaky.dto.product.ProductColorDTO;

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
    private List<String> sizes;
    private List<ProductColorDTO> colors;
    private String stockStatus;

}
