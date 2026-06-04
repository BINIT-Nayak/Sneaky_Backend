package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.entity.WishList;
import com.sneaky.sneaky.repository.CartRepository;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.repository.WishListRepository;
import com.sneaky.sneaky.services.analytics.ProductAnalyticsService;

@ExtendWith(MockitoExtension.class)
class ProductRecommendationServiceTest {

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private WishListRepository wishListRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductAnalyticsService productAnalyticsService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProductRecommendationService recommendationService;

    @Test
    void recommendationsUseCachedProductIdsWhenAvailable() {
        UUID userId = UUID.randomUUID();
        Products cachedProduct = product("Cached Pair", "Nike", "Running", BigDecimal.valueOf(12000), 1);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(listOperations.range("recommendations:user:" + userId, 0, 29))
                .thenReturn(List.of(cachedProduct.getProductId().toString()));
        when(valueOperations.get("recommendations:user:" + userId + ":personalized")).thenReturn("true");
        when(productsRepository.findByProductIdIn(List.of(cachedProduct.getProductId())))
                .thenReturn(List.of(cachedProduct));

        ProductRecommendationService.RecommendationResult result =
                recommendationService.getRecommendedProducts(userId);

        assertThat(result.personalized()).isTrue();
        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Cached Pair");
        verify(productsRepository, never()).findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED");
    }

    @Test
    void guestRecommendationsPreferMostViewedProducts() {
        Products newest = product("New Arrival", "Nike", "Runner", BigDecimal.valueOf(12000), 2);
        Products mostViewed = product("Popular Pair", "Adidas", "Lifestyle", BigDecimal.valueOf(9000), 1);

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED")).thenReturn(List.of(newest, mostViewed));
        when(productAnalyticsService.getMostViewedProductIds(100)).thenReturn(List.of(mostViewed.getProductId()));

        ProductRecommendationService.RecommendationResult result = recommendationService.getRecommendedProducts(null);

        assertThat(result.personalized()).isFalse();
        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Popular Pair", "New Arrival");
    }

    @Test
    void recommendationsExcludeAlreadyLoadedProducts() {
        Products alreadyLoaded = product("Already Loaded", "Nike", "Runner", BigDecimal.valueOf(12000), 2);
        Products nextProduct = product("Next Product", "Adidas", "Lifestyle", BigDecimal.valueOf(9000), 1);

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED"))
                .thenReturn(List.of(alreadyLoaded, nextProduct));

        ProductRecommendationService.RecommendationResult result =
                recommendationService.getRecommendedProducts(null, List.of(alreadyLoaded.getProductId()));

        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Next Product");
    }

    @Test
    void userRecommendationsStayGenericUntilEnoughPreferenceSignalsExist() {
        Users user = user();
        Products newest = product("New Arrival", "Nike", "Runner", BigDecimal.valueOf(12000), 2);
        Products mostViewed = product("Popular Pair", "Adidas", "Lifestyle", BigDecimal.valueOf(9000), 1);
        Products wishlistProduct = product("Saved Nike", "Nike", "Runner", BigDecimal.valueOf(10000), 3);
        WishList wishlist = WishList.builder()
                .user(user)
                .product(wishlistProduct)
                .build();

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED")).thenReturn(List.of(newest, mostViewed));
        when(productAnalyticsService.getMostViewedProductIds(100)).thenReturn(List.of(mostViewed.getProductId()));
        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of(wishlist));
        when(cartRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        when(productAnalyticsService.getRecentlyViewedProductIds(user.getUserId())).thenReturn(List.of());
        when(productAnalyticsService.getPassedProductIds(user.getUserId())).thenReturn(List.of());

        ProductRecommendationService.RecommendationResult result =
                recommendationService.getRecommendedProducts(user.getUserId());

        assertThat(result.personalized()).isFalse();
        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Popular Pair", "New Arrival");
    }

    @Test
    void userRecommendationsPreferProductsSimilarToWishlistHistory() {
        Users user = user();
        Products wishlistProduct = product("Saved Nike", "Nike", "Runner", BigDecimal.valueOf(10000), 3);
        Products recommendedNike = product("Recommended Nike", "Nike", "Runner", BigDecimal.valueOf(11000), 2);
        Products popularAdidas = product("Popular Adidas", "Adidas", "Lifestyle", BigDecimal.valueOf(9000), 1);
        WishList wishlist = WishList.builder()
                .user(user)
                .product(wishlistProduct)
                .build();

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED"))
                .thenReturn(List.of(popularAdidas, recommendedNike, wishlistProduct));
        when(productAnalyticsService.getMostViewedProductIds(100))
                .thenReturn(List.of(popularAdidas.getProductId()));
        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user))
                .thenReturn(wishlistSignals(user, wishlist, 19));
        when(cartRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        when(productAnalyticsService.getRecentlyViewedProductIds(user.getUserId())).thenReturn(List.of());

        ProductRecommendationService.RecommendationResult result =
                recommendationService.getRecommendedProducts(user.getUserId());

        assertThat(result.personalized()).isTrue();
        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Recommended Nike", "Popular Adidas", "Saved Nike");
    }

    @Test
    void userRecommendationsPenalizeCategoriesTheUserPassed() {
        Users user = user();
        Products passedRunner = product("Passed Runner", "Nike", "Running", BigDecimal.valueOf(10000), 3);
        Products anotherRunner = product("Another Runner", "Asics", "Running", BigDecimal.valueOf(11000), 2);
        Products lifestylePair = product("Lifestyle Pair", "Vans", "Lifestyle", BigDecimal.valueOf(9000), 1);

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED"))
                .thenReturn(List.of(anotherRunner, lifestylePair, passedRunner));
        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        when(cartRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        List<Products> passedProducts = passedSignals(passedRunner, 19);
        List<UUID> passedProductIds = passedProducts.stream()
                .map(Products::getProductId)
                .toList();
        when(productAnalyticsService.getPassedProductIds(user.getUserId())).thenReturn(passedProductIds);
        when(productsRepository.findByProductIdIn(passedProductIds))
                .thenReturn(passedProducts);

        ProductRecommendationService.RecommendationResult result =
                recommendationService.getRecommendedProducts(user.getUserId());

        assertThat(result.personalized()).isTrue();
        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Lifestyle Pair", "Another Runner", "Passed Runner");
    }

    @Test
    void userRecommendationsUseMerchantAffinity() {
        Users user = user();
        Products wishlistProduct = product("Saved Amazon Pair", "Nike", "Running", BigDecimal.valueOf(10000), 3);
        Products merchantMatch = product("Amazon Match", "Asics", "Lifestyle", BigDecimal.valueOf(19000), 2);
        Products neutralPair = product("Neutral Pair", "Vans", "Skate", BigDecimal.valueOf(19000), 1);
        WishList wishlist = WishList.builder()
                .user(user)
                .product(wishlistProduct)
                .build();

        wishlistProduct.setMerchantName("Amazon");
        merchantMatch.setMerchantName("Amazon");
        neutralPair.setMerchantName("Myntra");

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED"))
                .thenReturn(List.of(neutralPair, merchantMatch, wishlistProduct));
        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user))
                .thenReturn(wishlistSignals(user, wishlist, 19));
        when(cartRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        when(productAnalyticsService.getRecentlyViewedProductIds(user.getUserId())).thenReturn(List.of());
        when(productAnalyticsService.getPassedProductIds(user.getUserId())).thenReturn(List.of());

        ProductRecommendationService.RecommendationResult result =
                recommendationService.getRecommendedProducts(user.getUserId());

        assertThat(result.personalized()).isTrue();
        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Amazon Match", "Neutral Pair", "Saved Amazon Pair");
    }

    @Test
    void recommendationsDiversifyRepeatedCategoriesWhenScoresAreClose() {
        Products runningOne = product("Running One", "Nike", "Running", BigDecimal.valueOf(10000), 1);
        Products runningTwo = product("Running Two", "Adidas", "Running", BigDecimal.valueOf(10000), 2);
        Products trainingPair = product("Training Pair", "Puma", "Training", BigDecimal.valueOf(10000), 3);
        Products skatePair = product("Skate Pair", "Vans", "Skate", BigDecimal.valueOf(10000), 4);

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED"))
                .thenReturn(List.of(runningOne, runningTwo, trainingPair, skatePair));

        ProductRecommendationService.RecommendationResult result = recommendationService.getRecommendedProducts(null);

        assertThat(result.personalized()).isFalse();
        assertThat(result.products())
                .extracting(Products::getName)
                .containsExactly("Running One", "Training Pair", "Skate Pair", "Running Two");
    }

    @Test
    void recommendationsReturnAtMostThirtyProducts() {
        List<Products> products = new ArrayList<>();

        for (int index = 0; index < 35; index += 1) {
            products.add(product(
                    "Product " + index,
                    "Brand " + index,
                    "Category " + index,
                    BigDecimal.valueOf(1000 + index),
                    index));
        }

        when(productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc("APPROVED"))
                .thenReturn(products);

        ProductRecommendationService.RecommendationResult result = recommendationService.getRecommendedProducts(null);

        assertThat(result.products()).hasSize(30);
    }

    private static Users user() {
        Users user = new Users();
        user.setUserId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole("USER");
        return user;
    }

    private static List<WishList> wishlistSignals(Users user, WishList primaryWishlist, int extraCount) {
        List<WishList> wishlists = new ArrayList<>();
        wishlists.add(primaryWishlist);

        for (int index = 0; index < extraCount; index += 1) {
            Products neutralProduct = product(
                    "Neutral Wishlist " + index,
                    "Brand " + index,
                    "Category " + index,
                    BigDecimal.valueOf(1000 + index),
                    10 + index);
            neutralProduct.setMerchantName("Merchant " + index);
            wishlists.add(WishList.builder()
                    .user(user)
                    .product(neutralProduct)
                    .build());
        }

        return wishlists;
    }

    private static List<Products> passedSignals(Products primaryProduct, int extraCount) {
        List<Products> products = new ArrayList<>();
        products.add(primaryProduct);

        for (int index = 0; index < extraCount; index += 1) {
            Products neutralProduct = product(
                    "Neutral Passed " + index,
                    "Passed Brand " + index,
                    "Passed Category " + index,
                    BigDecimal.valueOf(1000 + index),
                    10 + index);
            neutralProduct.setMerchantName("Passed Merchant " + index);
            products.add(neutralProduct);
        }

        return products;
    }

    private static Products product(String name, String brandName, String category, BigDecimal price, int ageInDays) {
        Products product = new Products();
        product.setProductId(UUID.randomUUID());
        product.setName(name);
        product.setBrand(Brands.builder().id(UUID.nameUUIDFromBytes(brandName.getBytes())).name(brandName).build());
        product.setCategory(category);
        product.setPrice(price);
        product.setImageUrl(name + ".jpg");
        product.setDescription(name);
        product.setMerchantName("Amazon");
        product.setIsActive(true);
        product.setCreatedAt(LocalDateTime.now().minusDays(ageInDays));
        return product;
    }
}
