package com.sneaky.sneaky.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sneaky.sneaky.dto.analytics.ProductAnalyticsDTO;
import com.sneaky.sneaky.dto.product.ProductDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.ProductService;
import com.sneaky.sneaky.services.analytics.ProductAnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product-analytics")
@RequiredArgsConstructor
public class ProductAnalyticsController {
    private final ProductAnalyticsService productAnalyticsService;
    private final ProductService productService;
    private final CurrentUser currentUser;

    @GetMapping("/products/{productId}")
    public ProductAnalyticsDTO getProductAnalytics(@PathVariable UUID productId) {
        return productAnalyticsService.getProductAnalytics(productId);
    }

    @GetMapping("/recently-viewed")
    public List<ProductDTO> getRecentlyViewed() {
        List<UUID> productIds = productAnalyticsService.getRecentlyViewedProductIds(currentUser.getUserId());
        return productService.getProductsByIdsPreservingOrder(productIds);
    }
}
