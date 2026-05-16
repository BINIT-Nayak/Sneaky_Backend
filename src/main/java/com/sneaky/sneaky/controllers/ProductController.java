package com.sneaky.sneaky.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sneaky.sneaky.dto.product.*;
import com.sneaky.sneaky.services.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public List<ProductDTO> getProducts() {
        return productService.getActiveProducts();
    }

    @GetMapping("/recommended")
    public List<ProductDTO> getRecommendedProducts(Authentication authentication) {
        return productService.getRecommendedProducts(authenticatedUserId(authentication));
    }

    @GetMapping("/{id}")
    public ProductDTO getProduct(@PathVariable UUID id, Authentication authentication) {
        return productService.getProductById(id, authenticatedUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO createProduct(@Valid @RequestBody CreateProductRequestDTO request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequestDTO request) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}")
    public ProductDTO patchProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequestDTO request) {
        return productService.patchProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }

    private UUID authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
