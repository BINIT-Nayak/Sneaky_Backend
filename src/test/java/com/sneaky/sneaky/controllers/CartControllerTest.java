package com.sneaky.sneaky.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sneaky.sneaky.dto.cart.AddToCartRequestDTO;
import com.sneaky.sneaky.dto.cart.CartItemDTO;
import com.sneaky.sneaky.dto.cart.UpdateCartQuantityRequestDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.CartService;

class CartControllerTest {

    private final CartService cartService = org.mockito.Mockito.mock(CartService.class);
    private final CurrentUser currentUser = org.mockito.Mockito.mock(CurrentUser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService, currentUser)).build();
    }

    @Test
    void addToCartDelegatesToServiceForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(productId);
        request.setQuantity(2);

        when(currentUser.getUserId()).thenReturn(userId);
        when(cartService.addToCart(userId, productId, 2)).thenReturn(cartItem(productId, 2));

        mockMvc.perform(post("/api/cart")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.itemTotal").value(25998));

        verify(cartService).addToCart(userId, productId, 2);
    }

    @Test
    void getCartReturnsItemsForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(currentUser.getUserId()).thenReturn(userId);
        when(cartService.getCart(userId)).thenReturn(List.of(cartItem(productId, 1)));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$[0].name").value("Air Max"))
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[0].brandName").value("Nike"));
    }

    @Test
    void updateQuantityDelegatesToServiceForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UpdateCartQuantityRequestDTO request = new UpdateCartQuantityRequestDTO();
        request.setQuantity(3);

        when(currentUser.getUserId()).thenReturn(userId);
        when(cartService.updateQuantity(userId, productId, 3)).thenReturn(cartItem(productId, 3));

        mockMvc.perform(patch("/api/cart/{productId}", productId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.itemTotal").value(38997));

        verify(cartService).updateQuantity(userId, productId, 3);
    }

    @Test
    void removeAndClearCartDelegateToServiceForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(currentUser.getUserId()).thenReturn(userId);

        mockMvc.perform(delete("/api/cart/{productId}", productId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isNoContent());

        verify(cartService).removeFromCart(userId, productId);
        verify(cartService).clearCart(userId);
    }

    @Test
    void addToCartRejectsInvalidProductIdPayload() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType("application/json")
                        .content("{\"productId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest());

        verify(cartService, org.mockito.Mockito.never()).addToCart(any(), any(), any());
    }

    private static CartItemDTO cartItem(UUID productId, int quantity) {
        BigDecimal price = BigDecimal.valueOf(12999);
        return new CartItemDTO(
                productId,
                "Air Max",
                price,
                "INR",
                "image.jpg",
                "Nike",
                quantity,
                price.multiply(BigDecimal.valueOf(quantity)));
    }
}
