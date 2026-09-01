package com.sneaky.sneaky.services.analytics;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPreferenceProfileService {
    private static final double MIN_SCORE = -1.0;
    private static final double MAX_SCORE = 1.0;

    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserBrandPreferenceRepository userBrandPreferenceRepository;
    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    @Transactional
    public void applyEvent(UserActivityEventDTO event) {
        if (event == null || event.getUserId() == null || event.getProductId() == null || event.getEventType() == null) {
            return;
        }

        Users user = usersRepository.findById(event.getUserId()).orElse(null);
        Products product = productsRepository.findById(event.getProductId()).orElse(null);

        if (user == null || product == null) {
            return;
        }

        UserPreferences preferences = userPreferencesRepository.findById(user.getUserId())
                .orElseGet(() -> UserPreferences.builder().user(user).build());

        updateBehaviorStats(preferences, event.getEventType());

        double delta = EventWeights.normalizedDelta(event.getEventType());
        if (delta != 0.0) {
            updateBrandPreference(user, product.getBrand(), delta);
            updateCategoryPreference(user, product.getCategory(), delta);
        }

        if (EventWeights.weight(event.getEventType()) > 0) {
            updatePricePreference(preferences, product.getPrice());
        }

        userPreferencesRepository.save(preferences);
    }

    @Transactional(readOnly = true)
    public UserPreferenceProfile getProfile(UUID userId) {
        return userPreferencesRepository.findById(userId)
                .map(preferences -> new UserPreferenceProfile(
                        brandScores(userId),
                        categoryScores(userId),
                        preferences.getPreferredPriceMin(),
                        preferences.getPreferredPriceMax(),
                        preferences.totalInteractions()))
                .orElseGet(UserPreferenceProfile::empty);
    }

    private Map<String, Double> brandScores(UUID userId) {
        Map<String, Double> scores = new LinkedHashMap<>();

        userBrandPreferenceRepository.findByUserUserId(userId).forEach(preference -> {
            Brands brand = preference.getBrand();
            if (brand == null || brand.getId() == null) {
                return;
            }
            scores.put(
                    brand.getId().toString(),
                    clamp(preference.getScore() * PreferenceDecay.factor(preference.getUpdatedAt())));
        });

        return scores;
    }

    private Map<String, Double> categoryScores(UUID userId) {
        Map<String, Double> scores = new LinkedHashMap<>();

        userCategoryPreferenceRepository.findByUserUserId(userId).forEach(preference -> {
            if (preference.getCategory() == null || preference.getCategory().isBlank()) {
                return;
            }
            scores.put(
                    normalizeCategory(preference.getCategory()),
                    clamp(preference.getScore() * PreferenceDecay.factor(preference.getUpdatedAt())));
        });

        return scores;
    }

    private void updateBrandPreference(Users user, Brands brand, double delta) {
        if (brand == null || brand.getId() == null) {
            return;
        }

        UserBrandPreference preference = userBrandPreferenceRepository.findByUserAndBrand(user, brand)
                .orElseGet(() -> UserBrandPreference.builder()
                        .user(user)
                        .brand(brand)
                        .score(0.0)
                        .interactionCount(0)
                        .build());

        preference.setScore(updatedScore(preference.getScore(), preference.getUpdatedAt(), delta));
        preference.setInteractionCount(preference.getInteractionCount() + 1);
        userBrandPreferenceRepository.save(preference);
    }

    private void updateCategoryPreference(Users user, String category, double delta) {
        if (category == null || category.isBlank()) {
            return;
        }

        String normalizedCategory = normalizeCategory(category);
        UserCategoryPreference preference =
                userCategoryPreferenceRepository.findByUserAndCategoryIgnoreCase(user, normalizedCategory)
                        .orElseGet(() -> UserCategoryPreference.builder()
                                .user(user)
                                .category(normalizedCategory)
                                .score(0.0)
                                .interactionCount(0)
                                .build());

        preference.setScore(updatedScore(preference.getScore(), preference.getUpdatedAt(), delta));
        preference.setInteractionCount(preference.getInteractionCount() + 1);
        userCategoryPreferenceRepository.save(preference);
    }

    private static void updateBehaviorStats(UserPreferences preferences, UserActivityEventType eventType) {
        switch (eventType) {
            case IMPRESSION -> preferences.setImpressions(preferences.getImpressions() + 1);
            case VIEW -> preferences.setViews(preferences.getViews() + 1);
            case CLICK -> preferences.setClicks(preferences.getClicks() + 1);
            case SKIP -> preferences.setSkips(preferences.getSkips() + 1);
            case WISHLIST -> preferences.setWishlists(preferences.getWishlists() + 1);
            case CART -> preferences.setCarts(preferences.getCarts() + 1);
            case PURCHASE -> preferences.setPurchases(preferences.getPurchases() + 1);
        }
    }

    private static void updatePricePreference(UserPreferences preferences, BigDecimal price) {
        if (price == null) {
            return;
        }

        if (preferences.getPreferredPriceMin() == null || price.compareTo(preferences.getPreferredPriceMin()) < 0) {
            preferences.setPreferredPriceMin(price);
        }

        if (preferences.getPreferredPriceMax() == null || price.compareTo(preferences.getPreferredPriceMax()) > 0) {
            preferences.setPreferredPriceMax(price);
        }
    }

    private static double updatedScore(double currentScore, java.time.LocalDateTime updatedAt, double delta) {
        return clamp((currentScore * PreferenceDecay.factor(updatedAt)) + delta);
    }

    private static double clamp(double score) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    private static String normalizeCategory(String category) {
        return category.trim().toLowerCase(Locale.ROOT);
    }
}
