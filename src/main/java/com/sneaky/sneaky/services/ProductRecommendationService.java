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
    private static final double MERCHANT_MATCH_SCORE = 4.0;
    private static final double PRICE_MATCH_SCORE = 3.0;
    private static final double POPULARITY_SCORE = 2.0;
    private static final double PASSED_BRAND_PENALTY = 6.0;
    private static final double PASSED_CATEGORY_PENALTY = 10.0;
    private static final double PASSED_MERCHANT_PENALTY = 5.0;
    private static final double VIEWED_EXACT_PRODUCT_PENALTY = 12.0;
    private static final double PASSED_EXACT_PRODUCT_PENALTY = 35.0;
    private static final double OWNED_EXACT_PRODUCT_PENALTY = 50.0;
    private static final double CATEGORY_REPEAT_WINDOW_PENALTY = 7.0;
    private static final double BRAND_REPEAT_WINDOW_PENALTY = 3.0;
    private static final double MERCHANT_REPEAT_WINDOW_PENALTY = 2.0;
    private static final BigDecimal PRICE_RANGE_RATIO = BigDecimal.valueOf(0.25);
    private static final int POPULAR_PRODUCT_LIMIT = 100;
    private static final int DIVERSITY_WINDOW_SIZE = 4;
    private static final int RECOMMENDATION_CANDIDATE_LIMIT = 250;
    private static final int RECOMMENDATION_RESULT_LIMIT = 30;
    private static final int MIN_PERSONALIZATION_SIGNALS = 20;
    private static final String APPROVED_STATUS = "APPROVED";

    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;
    private final WishListRepository wishListRepository;
    private final CartRepository cartRepository;
    private final ProductAnalyticsService productAnalyticsService;

    @Transactional(readOnly = true)
    public RecommendationResult getRecommendedProducts(UUID userId) {
        List<Products> activeProducts =
                productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc(APPROVED_STATUS);

        if (activeProducts.isEmpty()) {
            return new RecommendationResult(List.of(), false);
        }

        Map<UUID, Integer> popularityRanks = popularityRanks();

        if (userId == null) {
            return new RecommendationResult(rankForGuest(activeProducts, popularityRanks), false);
        }

        return usersRepository.findById(userId)
                .map(user -> rankForUser(user, activeProducts, popularityRanks))
                .orElseGet(() -> new RecommendationResult(rankForGuest(activeProducts, popularityRanks), false));
    }

    private RecommendationResult rankForUser(Users user, List<Products> activeProducts, Map<UUID, Integer> popularityRanks) {
        List<Products> wishlistProducts = wishListRepository.findByUserWithProductAndBrand(user)
                .stream()
                .map(WishList::getProduct)
                .toList();
        List<Products> cartProducts = cartRepository.findByUserWithProductAndBrand(user)
                .stream()
                .map(Cart::getProduct)
                .toList();
        List<Products> recentlyViewedProducts = recentlyViewedProducts(user);
        List<Products> passedProducts = passedProducts(user);
        List<Products> signalProducts = new ArrayList<>();
        signalProducts.addAll(wishlistProducts);
        signalProducts.addAll(cartProducts);
        signalProducts.addAll(recentlyViewedProducts);

        Set<UUID> preferenceSignalIds = productIds(wishlistProducts);
        preferenceSignalIds.addAll(productIds(cartProducts));
        preferenceSignalIds.addAll(productIds(passedProducts));

        if (preferenceSignalIds.size() < MIN_PERSONALIZATION_SIGNALS) {
            return new RecommendationResult(rankForGuest(activeProducts, popularityRanks), false);
        }

        Set<UUID> ownedProductIds = productIds(wishlistProducts);
        ownedProductIds.addAll(productIds(cartProducts));
        Set<UUID> recentlyViewedProductIds = productIds(recentlyViewedProducts);
        Set<UUID> passedProductIds = productIds(passedProducts);

        List<ScoredProduct> scoredProducts = activeProducts.stream()
                .map(product -> new ScoredProduct(
                        product,
                        scoreForUser(
                                product,
                                signalProducts,
                                passedProducts,
                                ownedProductIds,
                                recentlyViewedProductIds,
                                passedProductIds,
                                popularityRanks)))
                .sorted(Comparator
                        .comparingDouble(ScoredProduct::score)
                        .reversed()
                        .thenComparing(
                                scoredProduct -> scoredProduct.product().getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECOMMENDATION_CANDIDATE_LIMIT)
                .toList();

        return new RecommendationResult(limitResults(diversify(scoredProducts)), true);
    }

    private List<Products> rankForGuest(List<Products> activeProducts, Map<UUID, Integer> popularityRanks) {
        List<ScoredProduct> scoredProducts = activeProducts.stream()
                .map(product -> new ScoredProduct(product, scorePopularity(product, popularityRanks)))
                .sorted(Comparator
                        .comparingDouble(ScoredProduct::score)
                        .reversed()
                        .thenComparing(
                                scoredProduct -> scoredProduct.product().getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECOMMENDATION_CANDIDATE_LIMIT)
                .toList();

        return limitResults(diversify(scoredProducts));
    }

    private List<Products> limitResults(List<Products> rankedProducts) {
        if (rankedProducts.size() <= RECOMMENDATION_RESULT_LIMIT) {
            return rankedProducts;
        }

        return rankedProducts.subList(0, RECOMMENDATION_RESULT_LIMIT);
    }

    private List<Products> diversify(List<ScoredProduct> scoredProducts) {
        List<ScoredProduct> remainingProducts = new ArrayList<>(scoredProducts);
        List<Products> rankedProducts = new ArrayList<>();

        while (!remainingProducts.isEmpty()) {
            int bestIndex = 0;
            double bestAdjustedScore = Double.NEGATIVE_INFINITY;

            for (int index = 0; index < remainingProducts.size(); index += 1) {
                ScoredProduct scoredProduct = remainingProducts.get(index);
                double adjustedScore = scoredProduct.score() - diversityPenalty(scoredProduct.product(), rankedProducts);

                if (adjustedScore > bestAdjustedScore) {
                    bestAdjustedScore = adjustedScore;
                    bestIndex = index;
                }
            }

            rankedProducts.add(remainingProducts.remove(bestIndex).product());
        }

        return rankedProducts;
    }

    private double diversityPenalty(Products product, List<Products> rankedProducts) {
        int fromIndex = Math.max(0, rankedProducts.size() - DIVERSITY_WINDOW_SIZE);
        double penalty = 0.0;

        for (Products recentProduct : rankedProducts.subList(fromIndex, rankedProducts.size())) {
            if (sameCategory(product, recentProduct)) {
                penalty += CATEGORY_REPEAT_WINDOW_PENALTY;
            }

            if (sameBrand(product, recentProduct)) {
                penalty += BRAND_REPEAT_WINDOW_PENALTY;
            }

            if (sameMerchant(product, recentProduct)) {
                penalty += MERCHANT_REPEAT_WINDOW_PENALTY;
            }
        }

        return penalty;
    }

    private double scoreForUser(
            Products product,
            List<Products> signalProducts,
            List<Products> passedProducts,
            Set<UUID> ownedProductIds,
            Set<UUID> recentlyViewedProductIds,
            Set<UUID> passedProductIds,
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

            if (sameMerchant(product, signalProduct)) {
                score += MERCHANT_MATCH_SCORE;
            }
        }

        for (Products passedProduct : passedProducts) {
            if (sameBrand(product, passedProduct)) {
                score -= PASSED_BRAND_PENALTY;
            }

            if (sameCategory(product, passedProduct)) {
                score -= PASSED_CATEGORY_PENALTY;
            }

            if (sameMerchant(product, passedProduct)) {
                score -= PASSED_MERCHANT_PENALTY;
            }
        }

        if (recentlyViewedProductIds.contains(product.getProductId())) {
            score -= VIEWED_EXACT_PRODUCT_PENALTY;
        }

        if (passedProductIds.contains(product.getProductId())) {
            score -= PASSED_EXACT_PRODUCT_PENALTY;
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

        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        return productsByIdsPreservingOrder(productIds);
    }

    private List<Products> passedProducts(Users user) {
        List<UUID> productIds;

        try {
            productIds = productAnalyticsService.getPassedProductIds(user.getUserId());
        } catch (RuntimeException e) {
            return List.of();
        }

        return productsByIdsPreservingOrder(productIds);
    }

    private List<Products> productsByIdsPreservingOrder(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Products> productsById = productsRepository.findByProductIdIn(productIds)
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

        if (productIds == null || productIds.isEmpty()) {
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

    private static boolean sameMerchant(Products product, Products signalProduct) {
        String merchantName = product.getMerchantName();
        String signalMerchantName = signalProduct.getMerchantName();

        return merchantName != null
                && signalMerchantName != null
                && !merchantName.isBlank()
                && merchantName.equalsIgnoreCase(signalMerchantName);
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

    private record ScoredProduct(Products product, double score) {
    }

    public record RecommendationResult(List<Products> products, boolean personalized) {
    }
}
