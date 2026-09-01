package com.sneaky.sneaky.services.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;
import com.sneaky.sneaky.dto.analytics.UserActivityEventType;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.UserBrandPreference;
import com.sneaky.sneaky.entity.UserCategoryPreference;
import com.sneaky.sneaky.entity.UserPreferences;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UserBrandPreferenceRepository;
import com.sneaky.sneaky.repository.UserCategoryPreferenceRepository;
import com.sneaky.sneaky.repository.UserPreferencesRepository;
import com.sneaky.sneaky.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class UserPreferenceProfileServiceTest {
    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserBrandPreferenceRepository userBrandPreferenceRepository;

    @Mock
    private UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    @Test
    void applyEventUpdatesProfileScoresStatsAndPriceRange() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Users user = user(userId);
        Brands brand = Brands.builder().id(UUID.randomUUID()).name("Nike").build();
        Products product = product(productId, brand, "Sneakers", BigDecimal.valueOf(12999));
        UserPreferenceProfileService service = service();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userPreferencesRepository.findById(userId)).thenReturn(Optional.empty());
        when(userBrandPreferenceRepository.findByUserAndBrand(user, brand)).thenReturn(Optional.empty());
        when(userCategoryPreferenceRepository.findByUserAndCategoryIgnoreCase(user, "sneakers")).thenReturn(Optional.empty());

        service.applyEvent(UserActivityEventDTO.builder()
                .eventType(UserActivityEventType.WISHLIST)
                .userId(userId)
                .productId(productId)
                .build());

        ArgumentCaptor<UserBrandPreference> brandCaptor = ArgumentCaptor.forClass(UserBrandPreference.class);
        ArgumentCaptor<UserCategoryPreference> categoryCaptor = ArgumentCaptor.forClass(UserCategoryPreference.class);
        ArgumentCaptor<UserPreferences> preferencesCaptor = ArgumentCaptor.forClass(UserPreferences.class);

        org.mockito.Mockito.verify(userBrandPreferenceRepository).save(brandCaptor.capture());
        org.mockito.Mockito.verify(userCategoryPreferenceRepository).save(categoryCaptor.capture());
        org.mockito.Mockito.verify(userPreferencesRepository).save(preferencesCaptor.capture());

        assertThat(brandCaptor.getValue().getScore()).isEqualTo(0.6);
        assertThat(categoryCaptor.getValue().getScore()).isEqualTo(0.6);
        assertThat(preferencesCaptor.getValue().getWishlists()).isEqualTo(1);
        assertThat(preferencesCaptor.getValue().getPreferredPriceMin()).isEqualByComparingTo("12999");
        assertThat(preferencesCaptor.getValue().getPreferredPriceMax()).isEqualByComparingTo("12999");
    }

    @Test
    void getProfileReturnsDecayedScores() {
        UUID userId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UserPreferences preferences = UserPreferences.builder()
                .user(user(userId))
                .impressions(2)
                .clicks(1)
                .build();
        UserBrandPreference brandPreference = UserBrandPreference.builder()
                .brand(Brands.builder().id(brandId).name("Nike").build())
                .score(0.5)
                .build();
        UserCategoryPreference categoryPreference = UserCategoryPreference.builder()
                .category("Sneakers")
                .score(-0.5)
                .build();
        UserPreferenceProfileService service = service();

        when(userPreferencesRepository.findById(userId)).thenReturn(Optional.of(preferences));
        when(userBrandPreferenceRepository.findByUserUserId(userId)).thenReturn(List.of(brandPreference));
        when(userCategoryPreferenceRepository.findByUserUserId(userId)).thenReturn(List.of(categoryPreference));

        UserPreferenceProfile profile = service.getProfile(userId);

        assertThat(profile.totalInteractions()).isEqualTo(3);
        assertThat(profile.brandScores()).containsEntry(brandId.toString(), 0.5);
        assertThat(profile.categoryScores()).containsEntry("sneakers", -0.5);
    }

    private UserPreferenceProfileService service() {
        return new UserPreferenceProfileService(
                usersRepository,
                productsRepository,
                userPreferencesRepository,
                userBrandPreferenceRepository,
                userCategoryPreferenceRepository);
    }

    private static Users user(UUID userId) {
        Users user = new Users();
        user.setUserId(userId);
        user.setEmail("test@example.com");
        user.setPassword("password");
        return user;
    }

    private static Products product(UUID productId, Brands brand, String category, BigDecimal price) {
        Products product = new Products();
        product.setProductId(productId);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(price);
        return product;
    }
}
