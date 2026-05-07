package com.sneaky.sneaky.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sneaky.sneaky.dto.cart.AddToCartRequestDTO;
import com.sneaky.sneaky.dto.cart.CartItemDTO;
import com.sneaky.sneaky.dto.cart.UpdateCartQuantityRequestDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.CartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemDTO addToCart(@RequestBody AddToCartRequestDTO request) {
        return cartService.addToCart(currentUser.getUserId(), request.getProductId(), request.getQuantity());
    }

    @GetMapping
    public List<CartItemDTO> getCart() {
        return cartService.getCart(currentUser.getUserId());
    }

    @PatchMapping("/{productId}")
    public CartItemDTO updateQuantity(
            @PathVariable UUID productId,
            @RequestBody UpdateCartQuantityRequestDTO request) {
        return cartService.updateQuantity(currentUser.getUserId(), productId, request.getQuantity());
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromCart(@PathVariable UUID productId) {
        cartService.removeFromCart(currentUser.getUserId(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart() {
        cartService.clearCart(currentUser.getUserId());
    }
}
