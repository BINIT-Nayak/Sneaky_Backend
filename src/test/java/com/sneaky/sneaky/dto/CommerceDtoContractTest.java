package com.sneaky.sneaky.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sneaky.sneaky.dto.cart.AddToCartRequestDTO;
import com.sneaky.sneaky.dto.cart.CartItemDTO;
import com.sneaky.sneaky.dto.cart.UpdateCartQuantityRequestDTO;
import com.sneaky.sneaky.dto.product.CreateProductRequestDTO;
import com.sneaky.sneaky.dto.product.ProductColorDTO;
import com.sneaky.sneaky.dto.product.ProductDTO;
import com.sneaky.sneaky.dto.wishlist.AddToWishlistRequestDTO;
import com.sneaky.sneaky.dto.wishlist.WishlistItemDTO;

class CommerceDtoContractTest {

    @Test
    void createProductDefaultsToActive() {
        CreateProductRequestDTO request = new CreateProductRequestDTO();

        assertThat(request.getIsActive()).isTrue();
    }

    @Test
    void productDtoExposesFrontendImageField() {
        UUID productId = UUID.randomUUID();
        ProductDTO product = new ProductDTO(
                productId,
                "Air Max",
                BigDecimal.valueOf(12999),
                "image.jpg",
                "Comfortable",
                "Nike",
                "Sneakers",
                List.of("UK 8", "UK 9"),
                List.of(new ProductColorDTO("Black", "#17151d")),
                "Only a few left");

        assertThat(product.getId()).isEqualTo(productId);
        assertThat(product.getImage()).isEqualTo("image.jpg");
        assertThat(product.getBrand()).isEqualTo("Nike");
        assertThat(product.getSizes()).containsExactly("UK 8", "UK 9");
        assertThat(product.getColors()).singleElement().extracting("value").isEqualTo("#17151d");
        assertThat(product.getStockStatus()).isEqualTo("Only a few left");
    }

    @Test
    void cartRequestAndResponseDtosCarryQuantityAndTotals() {
        UUID productId = UUID.randomUUID();
        AddToCartRequestDTO addRequest = new AddToCartRequestDTO();
        UpdateCartQuantityRequestDTO updateRequest = new UpdateCartQuantityRequestDTO();
        CartItemDTO cartItem = new CartItemDTO(
                productId,
                "Air Max",
                BigDecimal.valueOf(12999),
                "INR",
                "image.jpg",
                "Nike",
                2,
                BigDecimal.valueOf(25998),
                List.of("UK 8", "UK 9"),
                List.of(new ProductColorDTO("Black", "#17151d")),
                "In stock");

        addRequest.setProductId(productId);
        addRequest.setQuantity(2);
        updateRequest.setQuantity(3);

        assertThat(addRequest.getProductId()).isEqualTo(productId);
        assertThat(addRequest.getQuantity()).isEqualTo(2);
        assertThat(updateRequest.getQuantity()).isEqualTo(3);
        assertThat(cartItem.getProductId()).isEqualTo(productId);
        assertThat(cartItem.getQuantity()).isEqualTo(2);
        assertThat(cartItem.getItemTotal()).isEqualByComparingTo(BigDecimal.valueOf(25998));
    }

    @Test
    void wishlistDtosExposeProductIdentityAndBrandName() {
        UUID productId = UUID.randomUUID();
        AddToWishlistRequestDTO request = new AddToWishlistRequestDTO();
        WishlistItemDTO item = new WishlistItemDTO(
                productId,
                "Air Max",
                BigDecimal.valueOf(12999),
                "image.jpg",
                "Nike",
                List.of("UK 8", "UK 9"),
                List.of(new ProductColorDTO("Black", "#17151d")),
                "In stock");

        request.setProductId(productId);

        assertThat(request.getProductId()).isEqualTo(productId);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getBrandName()).isEqualTo("Nike");
        assertThat(item.getImageUrl()).isEqualTo("image.jpg");
    }
}
