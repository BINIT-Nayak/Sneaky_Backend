package com.sneaky.sneaky.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sneaky.sneaky.dto.wishlist.AddToWishlistRequestDTO;
import com.sneaky.sneaky.dto.wishlist.WishlistItemDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.WishlistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addToWishlist(@RequestBody AddToWishlistRequestDTO request) {
        wishlistService.addToWishlist(currentUser.getUserId(), request.getProductId());
    }

    @GetMapping
    public List<WishlistItemDTO> getWishlist() {
        return wishlistService.getWishlist(currentUser.getUserId());
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWishlist(@PathVariable UUID productId) {
        wishlistService.removeFromWishlist(currentUser.getUserId(), productId);
    }
}