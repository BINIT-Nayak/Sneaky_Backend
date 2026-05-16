package com.sneaky.sneaky.dto.cart;

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
public class CartItemDTO {
    private UUID productId;
    private String name;
    private BigDecimal price;
    private String currency;
    private String imageUrl;
    private String brandName;
    private String category;
    private String merchantName;
    private String merchantUrl;
    private Integer quantity;
    private BigDecimal itemTotal;
    private List<String> sizes;
    private List<ProductColorDTO> colors;
    private String stockStatus;
}
