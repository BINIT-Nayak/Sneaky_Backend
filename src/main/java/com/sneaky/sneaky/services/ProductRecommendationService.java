package com.sneaky.sneaky.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
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
import com.sneaky.sneaky.services.recommendation.MlRankingClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private static final double ML_PROBABILITY_SCORE_SCALE = 100.0;
    private static final BigDecimal PRICE_RANGE_RATIO = BigDecimal.valueOf(0.25);
    private static final int POPULAR_PRODUCT_LIMIT = 100;
    private static final int DIVERSITY_WINDOW_SIZE = 4;
    private static final int RECOMMENDATION_CANDIDATE_LIMIT = 250;
    private static final int RECOMMENDATION_RESULT_LIMIT = 30;
    private static final int MIN_PERSONALIZATION_SIGNALS = 20;
    private static final Duration RECOMMENDATION_CACHE_TTL = Duration.ofMinutes(15);
    private static final String APPROVED_STATUS = "APPROVED";
    private static final String GUEST_RECOMMENDATIONS_KEY = "recommendations:guest";
    private static final String PERSONALIZED_SUFFIX = ":personalized";

    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;
    private final WishListRepository wishListRepository;
    private final CartRepository cartRepository;
    private final ProductAnalyticsService productAnalyticsService;
    private final StringRedisTemplate redisTemplate;
    private final MlRankingClient mlRankingClient;

    @Transactional(readOnly = true)
    public RecommendationResult getRecommendedProducts(UUID userId) {
        return getRecommendedProducts(userId, Set.of());
    }

    @Transactional(readOnly = true)
    public RecommendationResult getRecommendedProducts(UUID userId, List<UUID> excludedProductIds) {
        return getRecommendedProducts(userId, normalizedExcludedProductIds(excludedProductIds));
    }

    private RecommendationResult getRecommendedProducts(UUID userId, Set<UUID> excludedProductIds) {
        if (!excludedProductIds.isEmpty()) {
            log.info(
                    "Recommendation cache bypassed because excluded product ids were provided. audience={}, excludedCount={}",
                    audience(userId),
                    excludedProductIds.size());
            return computeRecommendedProducts(userId, excludedProductIds);
        }

        RecommendationResult cachedResult = cachedRecommendations(userId);
        if (cachedResult != null) {
            return cachedResult;
        }

        RecommendationResult recommendationResult = computeRecommendedProducts(userId, excludedProductIds);
        cacheRecommendations(userId, recommendationResult);
        return recommendationResult;
    }

    @Transactional(readOnly = true)
    public RecommendationResult refreshRecommendedProducts(UUID userId) {
        RecommendationResult recommendationResult = computeRecommendedProducts(userId, Set.of());
        cacheRecommendations(userId, recommendationResult);
        return recommendationResult;
    }

    private RecommendationResult computeRecommendedProducts(UUID userId, Set<UUID> excludedProductIds) {
        List<Products> activeProducts =
                productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc(APPROVED_STATUS)
                        .stream()
                        .filter(product -> !excludedProductIds.contains(product.getProductId()))
                        .toList();

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

    private RecommendationResult cachedRecommendations(UUID userId) {
        String key = recommendationsKey(userId);

        try {
            List<String> productIds = redisTemplate.opsForList().range(key, 0, RECOMMENDATION_RESULT_LIMIT - 1);

            if (productIds == null || productIds.isEmpty()) {
                log.info("Recommendation Redis cache miss. key={}, audience={}", key, audience(userId));
                return null;
            }

            List<UUID> parsedProductIds = productIds.stream()
                    .map(UUID::fromString)
                    .toList();
            List<Products> products = productsByIdsPreservingOrder(parsedProductIds);

            if (products.isEmpty()) {
                log.info(
                        "Recommendation Redis cache ignored because cached product ids no longer resolve. key={}, cachedCount={}",
                        key,
                        productIds.size());
                return null;
            }

            log.info(
                    "Recommendation Redis cache hit. key={}, audience={}, cachedCount={}, resolvedCount={}",
                    key,
                    audience(userId),
                    productIds.size(),
                    products.size());
            return new RecommendationResult(products, cachedPersonalized(key));
        } catch (RuntimeException e) {
            log.warn("Recommendation Redis cache read failed. key={}, audience={}", key, audience(userId), e);
            return null;
        }
    }

    private boolean cachedPersonalized(String key) {
        String personalized = redisTemplate.opsForValue().get(personalizedKey(key));
        return Boolean.parseBoolean(personalized);
    }

    private void cacheRecommendations(UUID userId, RecommendationResult recommendationResult) {
        String key = recommendationsKey(userId);

        try {
            redisTemplate.delete(key);

            List<String> productIds = recommendationResult.products()
                    .stream()
                    .map(product -> product.getProductId().toString())
                    .toList();

            if (!productIds.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(key, productIds);
            }

            redisTemplate.expire(key, RECOMMENDATION_CACHE_TTL);
            redisTemplate.opsForValue().set(
                    personalizedKey(key),
                    String.valueOf(recommendationResult.personalized()),
                    RECOMMENDATION_CACHE_TTL);
            log.info(
                    "Recommendation Redis cache written. key={}, audience={}, productCount={}, ttlSeconds={}, personalized={}",
                    key,
                    audience(userId),
                    productIds.size(),
                    RECOMMENDATION_CACHE_TTL.toSeconds(),
                    recommendationResult.personalized());
        } catch (RuntimeException e) {
            log.warn("Recommendation Redis cache write failed. key={}, audience={}", key, audience(userId), e);
        }
    }

    private String recommendationsKey(UUID userId) {
        return userId == null
                ? GUEST_RECOMMENDATIONS_KEY
                : "recommendations:user:" + userId;
    }

    private static String personalizedKey(String recommendationsKey) {
        return recommendationsKey + PERSONALIZED_SUFFIX;
    }

    private static String audience(UUID userId) {
        return userId == null ? "guest" : "user";
    }

    private static Set<UUID> normalizedExcludedProductIds(List<UUID> excludedProductIds) {
        if (excludedProductIds == null || excludedProductIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(excludedProductIds);
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
                .map(product -> scoredProductForUser(
                        product,
                        signalProducts,
                        passedProducts,
                        ownedProductIds,
                        recentlyViewedProductIds,
                        passedProductIds,
                        popularityRanks))
                .sorted(Comparator
                        .comparingDouble(ScoredProduct::score)
                        .reversed()
                        .thenComparing(
                                scoredProduct -> scoredProduct.product().getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECOMMENDATION_CANDIDATE_LIMIT)
                .toList();

        return new RecommendationResult(limitResults(diversify(mlRerank(user, scoredProducts))), true);
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

    private ScoredProduct scoredProductForUser(
            Products product,
            List<Products> signalProducts,
            List<Products> passedProducts,
            Set<UUID> ownedProductIds,
            Set<UUID> recentlyViewedProductIds,
            Set<UUID> passedProductIds,
            Map<UUID, Integer> popularityRanks) {
        double score = scorePopularity(product, popularityRanks);
        int brandMatches = 0;
        int categoryMatches = 0;
        int merchantMatches = 0;
        int priceMatches = 0;
        int passedBrandMatches = 0;
        int passedCategoryMatches = 0;
        int passedMerchantMatches = 0;

        for (Products signalProduct : signalProducts) {
            if (sameBrand(product, signalProduct)) {
                brandMatches += 1;
                score += BRAND_MATCH_SCORE;
            }

            if (sameCategory(product, signalProduct)) {
                categoryMatches += 1;
                score += CATEGORY_MATCH_SCORE;
            }

            if (similarPrice(product, signalProduct)) {
                priceMatches += 1;
                score += PRICE_MATCH_SCORE;
            }

            if (sameMerchant(product, signalProduct)) {
                merchantMatches += 1;
                score += MERCHANT_MATCH_SCORE;
            }
        }

        for (Products passedProduct : passedProducts) {
            if (sameBrand(product, passedProduct)) {
                passedBrandMatches += 1;
                score -= PASSED_BRAND_PENALTY;
            }

            if (sameCategory(product, passedProduct)) {
                passedCategoryMatches += 1;
                score -= PASSED_CATEGORY_PENALTY;
            }

            if (sameMerchant(product, passedProduct)) {
                passedMerchantMatches += 1;
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

        int popularityRank = popularityRanks.getOrDefault(product.getProductId(), POPULAR_PRODUCT_LIMIT);
        double price = product.getPrice() == null ? 0.0 : product.getPrice().doubleValue();
        MlRankingClient.CandidateFeatures features = new MlRankingClient.CandidateFeatures(
                product.getProductId(),
                score,
                price,
                popularityRank,
                brandMatches,
                categoryMatches,
                merchantMatches,
                priceMatches,
                passedBrandMatches,
                passedCategoryMatches,
                passedMerchantMatches,
                recentlyViewedProductIds.contains(product.getProductId()),
                passedProductIds.contains(product.getProductId()),
                ownedProductIds.contains(product.getProductId()));

        return new ScoredProduct(product, score, features);
    }

    private List<ScoredProduct> mlRerank(Users user, List<ScoredProduct> candidates) {
        List<MlRankingClient.CandidateFeatures> features = candidates.stream()
                .map(ScoredProduct::features)
                .toList();

        return mlRankingClient.rank(user.getUserId(), features)
                .map(rankings -> {
                    Map<UUID, Double> scores = new LinkedHashMap<>();
                    rankings.forEach(ranking -> scores.put(ranking.productId(), ranking.score()));
                    return candidates.stream()
                            .map(candidate -> new ScoredProduct(
                                    candidate.product(),
                                    scores.get(candidate.product().getProductId()) * ML_PROBABILITY_SCORE_SCALE,
                                    candidate.features()))
                            .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed())
                            .toList();
                })
                .orElse(candidates);
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

    private record ScoredProduct(Products product, double score, MlRankingClient.CandidateFeatures features) {
        private ScoredProduct(Products product, double score) {
            this(product, score, null);
        }
    }

    public record RecommendationResult(List<Products> products, boolean personalized) {
    }
}
