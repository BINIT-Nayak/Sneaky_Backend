package com.sneaky.sneaky.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.sneaky.sneaky.dto.wishlist.AddToWishlistRequestDTO;
import com.sneaky.sneaky.dto.wishlist.WishlistItemDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.WishlistService;

class WishlistControllerTest {

    private final WishlistService wishlistService = org.mockito.Mockito.mock(WishlistService.class);
    private final CurrentUser currentUser = org.mockito.Mockito.mock(CurrentUser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WishlistController(wishlistService, currentUser)).build();
    }

    @Test
    void addToWishlistDelegatesToServiceForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AddToWishlistRequestDTO request = new AddToWishlistRequestDTO();
        request.setProductId(productId);

        when(currentUser.getUserId()).thenReturn(userId);

        mockMvc.perform(post("/api/wishlist")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(wishlistService).addToWishlist(userId, productId);
    }

    @Test
    void getWishlistReturnsItemsForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        WishlistItemDTO item = new WishlistItemDTO(
                productId,
                "Air Max",
                BigDecimal.valueOf(12999),
                "image.jpg",
                "Nike");

        when(currentUser.getUserId()).thenReturn(userId);
        when(wishlistService.getWishlist(userId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$[0].name").value("Air Max"))
                .andExpect(jsonPath("$[0].price").value(12999))
                .andExpect(jsonPath("$[0].imageUrl").value("image.jpg"))
                .andExpect(jsonPath("$[0].brandName").value("Nike"));
    }

    @Test
    void removeFromWishlistDelegatesToServiceForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(currentUser.getUserId()).thenReturn(userId);

        mockMvc.perform(delete("/api/wishlist/{productId}", productId))
                .andExpect(status().isNoContent());

        verify(wishlistService).removeFromWishlist(userId, productId);
    }

    @Test
    void addToWishlistRejectsInvalidProductIdPayload() throws Exception {
        mockMvc.perform(post("/api/wishlist")
                        .contentType("application/json")
                        .content("{\"productId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest());

        verify(wishlistService, org.mockito.Mockito.never()).addToWishlist(any(), any());
    }
}
