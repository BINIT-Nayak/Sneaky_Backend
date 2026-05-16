package com.sneaky.sneaky.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.entity.WishList;
import com.sneaky.sneaky.repository.CartRepository;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.repository.WishListRepository;
import com.sneaky.sneaky.services.analytics.ProductAnalyticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductRecommendationService {
    private static final double BRAND_MATCH_SCORE = 8.0;
    private static final double CATEGORY_MATCH_SCORE = 5.0;
    private static final double PRICE_MATCH_SCORE = 3.0;
    private static final double POPULARITY_SCORE = 2.0;
    private static final double VIEWED_EXACT_PRODUCT_PENALTY = 12.0;
    private static final double OWNED_EXACT_PRODUCT_PENALTY = 50.0;
    private static final BigDecimal PRICE_RANGE_RATIO = BigDecimal.valueOf(0.25);
    private static final int POPULAR_PRODUCT_LIMIT = 100;

    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;
    private final WishListRepository wishListRepository;
    private final CartRepository cartRepository;
    private final ProductAnalyticsService productAnalyticsService;

    @Transactional(readOnly = true)
    public List<Products> getRecommendedProducts(UUID userId) {
        List<Products> activeProducts = productsRepository.findByIsActiveTrueOrderByCreatedAtDesc();

        if (activeProducts.isEmpty()) {
            return List.of();
        }

        Map<UUID, Integer> popularityRanks = popularityRanks();

        if (userId == null) {
            return rankForGuest(activeProducts, popularityRanks);
        }

        return usersRepository.findById(userId)
                .map(user -> rankForUser(user, activeProducts, popularityRanks))
                .orElseGet(() -> rankForGuest(activeProducts, popularityRanks));
    }

    private List<Products> rankForUser(Users user, List<Products> activeProducts, Map<UUID, Integer> popularityRanks) {
        List<Products> wishlistProducts = wishListRepository.findByUserWithProductAndBrand(user)
                .stream()
                .map(WishList::getProduct)
                .toList();
        List<Products> cartProducts = cartRepository.findByUserWithProductAndBrand(user)
                .stream()
                .map(Cart::getProduct)
                .toList();
        List<Products> recentlyViewedProducts = recentlyViewedProducts(user);
        List<Products> signalProducts = new ArrayList<>();
        signalProducts.addAll(wishlistProducts);
        signalProducts.addAll(cartProducts);
        signalProducts.addAll(recentlyViewedProducts);

        if (signalProducts.isEmpty()) {
            return rankForGuest(activeProducts, popularityRanks);
        }

        Set<UUID> ownedProductIds = productIds(wishlistProducts);
        ownedProductIds.addAll(productIds(cartProducts));
        Set<UUID> recentlyViewedProductIds = productIds(recentlyViewedProducts);

        return activeProducts.stream()
                .sorted(Comparator
                        .comparingDouble((Products product) -> scoreForUser(
                                product,
                                signalProducts,
                                ownedProductIds,
                                recentlyViewedProductIds,
                                popularityRanks))
                        .reversed()
                        .thenComparing(Products::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<Products> rankForGuest(List<Products> activeProducts, Map<UUID, Integer> popularityRanks) {
        return activeProducts.stream()
                .sorted(Comparator
                        .comparingDouble((Products product) -> scorePopularity(product, popularityRanks))
                        .reversed()
                        .thenComparing(Products::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private double scoreForUser(
            Products product,
            List<Products> signalProducts,
            Set<UUID> ownedProductIds,
            Set<UUID> recentlyViewedProductIds,
            Map<UUID, Integer> popularityRanks) {
        double score = scorePopularity(product, popularityRanks);

        for (Products signalProduct : signalProducts) {
            if (sameBrand(product, signalProduct)) {
                score += BRAND_MATCH_SCORE;
            }

            if (sameCategory(product, signalProduct)) {
                score += CATEGORY_MATCH_SCORE;
            }

            if (similarPrice(product, signalProduct)) {
                score += PRICE_MATCH_SCORE;
            }
        }

        if (recentlyViewedProductIds.contains(product.getProductId())) {
            score -= VIEWED_EXACT_PRODUCT_PENALTY;
        }

        if (ownedProductIds.contains(product.getProductId())) {
            score -= OWNED_EXACT_PRODUCT_PENALTY;
        }

        return score;
    }

    private double scorePopularity(Products product, Map<UUID, Integer> popularityRanks) {
        Integer rank = popularityRanks.get(product.getProductId());

        if (rank == null) {
            return 0.0;
        }

        return POPULARITY_SCORE / (rank + 1);
    }

    private List<Products> recentlyViewedProducts(Users user) {
        List<UUID> productIds;

        try {
            productIds = productAnalyticsService.getRecentlyViewedProductIds(user.getUserId());
        } catch (RuntimeException e) {
            return List.of();
        }

        if (productIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Products> productsById = productsRepository.findAllById(productIds)
                .stream()
                .collect(LinkedHashMap::new, (map, product) -> map.put(product.getProductId(), product), Map::putAll);

        return productIds.stream()
                .map(productsById::get)
                .filter(product -> product != null)
                .toList();
    }

    private Map<UUID, Integer> popularityRanks() {
        List<UUID> productIds;

        try {
            productIds = productAnalyticsService.getMostViewedProductIds(POPULAR_PRODUCT_LIMIT);
        } catch (RuntimeException e) {
            return Map.of();
        }

        Map<UUID, Integer> ranks = new LinkedHashMap<>();

        for (int index = 0; index < productIds.size(); index += 1) {
            ranks.put(productIds.get(index), index);
        }

        return ranks;
    }

    private static Set<UUID> productIds(List<Products> products) {
        Set<UUID> productIds = new HashSet<>();

        for (Products product : products) {
            productIds.add(product.getProductId());
        }

        return productIds;
    }

    private static boolean sameBrand(Products product, Products signalProduct) {
        Brands brand = product.getBrand();
        Brands signalBrand = signalProduct.getBrand();

        return brand != null
                && signalBrand != null
                && brand.getId() != null
                && brand.getId().equals(signalBrand.getId());
    }

    private static boolean sameCategory(Products product, Products signalProduct) {
        String category = product.getCategory();
        String signalCategory = signalProduct.getCategory();

        return category != null
                && signalCategory != null
                && category.equalsIgnoreCase(signalCategory);
    }

    private static boolean similarPrice(Products product, Products signalProduct) {
        BigDecimal price = product.getPrice();
        BigDecimal signalPrice = signalProduct.getPrice();

        if (price == null || signalPrice == null || signalPrice.signum() <= 0) {
            return false;
        }

        BigDecimal difference = price.subtract(signalPrice).abs();
        BigDecimal allowedDifference = signalPrice.multiply(PRICE_RANGE_RATIO);

        return difference.compareTo(allowedDifference) <= 0;
    }
}
