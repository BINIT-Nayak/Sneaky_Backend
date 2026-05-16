package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @InjectMocks
    private ProductRecommendationService recommendationService;

    @Test
    void guestRecommendationsPreferMostViewedProducts() {
        Products newest = product("New Arrival", "Nike", "Runner", BigDecimal.valueOf(12000), 2);
        Products mostViewed = product("Popular Pair", "Adidas", "Lifestyle", BigDecimal.valueOf(9000), 1);

        when(productsRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(newest, mostViewed));
        when(productAnalyticsService.getMostViewedProductIds(100)).thenReturn(List.of(mostViewed.getProductId()));

        assertThat(recommendationService.getRecommendedProducts(null))
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

        when(productsRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(popularAdidas, recommendedNike, wishlistProduct));
        when(productAnalyticsService.getMostViewedProductIds(100))
                .thenReturn(List.of(popularAdidas.getProductId()));
        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of(wishlist));
        when(cartRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        when(productAnalyticsService.getRecentlyViewedProductIds(user.getUserId())).thenReturn(List.of());

        assertThat(recommendationService.getRecommendedProducts(user.getUserId()))
                .extracting(Products::getName)
                .containsExactly("Recommended Nike", "Popular Adidas", "Saved Nike");
    }

    @Test
    void userRecommendationsPenalizeCategoriesTheUserPassed() {
        Users user = user();
        Products passedRunner = product("Passed Runner", "Nike", "Running", BigDecimal.valueOf(10000), 3);
        Products anotherRunner = product("Another Runner", "Asics", "Running", BigDecimal.valueOf(11000), 2);
        Products lifestylePair = product("Lifestyle Pair", "Vans", "Lifestyle", BigDecimal.valueOf(9000), 1);

        when(productsRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(anotherRunner, lifestylePair, passedRunner));
        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        when(cartRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of());
        when(productAnalyticsService.getPassedProductIds(user.getUserId()))
                .thenReturn(List.of(passedRunner.getProductId()));
        when(productsRepository.findAllById(List.of(passedRunner.getProductId())))
                .thenReturn(List.of(passedRunner));

        assertThat(recommendationService.getRecommendedProducts(user.getUserId()))
                .extracting(Products::getName)
                .containsExactly("Lifestyle Pair", "Another Runner", "Passed Runner");
    }

    private static Users user() {
        Users user = new Users();
        user.setUserId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole("USER");
        return user;
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
        product.setIsActive(true);
        product.setCreatedAt(LocalDateTime.now().minusDays(ageInDays));
        return product;
    }
}
