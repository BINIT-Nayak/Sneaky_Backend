package com.sneaky.sneaky.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.ProductColor;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.repository.BrandsRepository;
import com.sneaky.sneaky.repository.ProductsRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.products.enabled", havingValue = "true", matchIfMissing = true)
public class ProductCatalogSeeder {

    private static final String[] BRAND_NAMES = {
            "Nike", "Adidas", "Puma", "Reebok", "New Balance", "Asics", "Converse", "Vans"
    };

    private static final String[] MODEL_NAMES = {
            "Air Pulse Runner", "Court Legacy Low", "Street Glide", "Retro Sprint", "Cloud Tempo",
            "Metro Flex", "Daily Lift", "Urban Trail", "Prime Step", "Classic Wave",
            "Velocity Knit", "Summit Runner", "Canvas Deck", "Arc Trainer", "Nova Court",
            "Rush Runner", "Ease Walk", "Shadow Low", "Orbit Lace", "Fresh Foam",
            "City Runner", "Skate Core", "Runner Pro", "Leather Court", "Aero Glide",
            "Terrace Low", "Track Lite", "Studio Step", "Rapid Mesh", "Heritage High",
            "Neo Runner", "Balance Court", "Flex Runner", "Street Runner", "Tonal Low",
            "Motion Max", "Court Prime", "Lite Runner", "Sprint Deck", "Gel Street",
            "Daily Court", "Active Mesh", "Urban Runner", "Cloud Lift", "Skate Low",
            "Retro High", "Training Plus", "Runner Elite", "Court Soft", "Trail City"
    };

    private static final String[] IMAGE_URLS = {
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1543508282-6319a3e2621f?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1491553895911-0055eca6402d?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=900&q=80"
    };

    private static final List<List<String>> SIZE_SETS = List.of(
            List.of("UK 5", "UK 6", "UK 7", "UK 8"),
            List.of("UK 6", "UK 7", "UK 8", "UK 9", "UK 10"),
            List.of("UK 7", "UK 8", "UK 9", "UK 10", "UK 11"),
            List.of("UK 4", "UK 5", "UK 6", "UK 7"),
            List.of("UK 8", "UK 9", "UK 10", "UK 11", "UK 12"));

    private static final List<List<ProductColor>> COLOR_SETS = List.of(
            List.of(new ProductColor("Black", "#17151d"), new ProductColor("Ivory", "#eee4cf")),
            List.of(new ProductColor("White", "#f8f7f2"), new ProductColor("Green", "#2f6f4e")),
            List.of(new ProductColor("Navy", "#1c2942"), new ProductColor("Grey", "#8f949b")),
            List.of(new ProductColor("Clay", "#c27a58"), new ProductColor("Cream", "#f3dfbd")),
            List.of(new ProductColor("Red", "#bb2f36"), new ProductColor("Black", "#151515")),
            List.of(new ProductColor("Blue", "#2f5f9f"), new ProductColor("Silver", "#c8c9cb")),
            List.of(new ProductColor("Tan", "#b88a5a"), new ProductColor("Brown", "#6a4a32")),
            List.of(new ProductColor("Lilac", "#a99ad6"), new ProductColor("White", "#fafafa")));

    private final ProductsRepository productsRepository;
    private final BrandsRepository brandsRepository;

    @Bean
    ApplicationRunner seedProducts(@Value("${app.seed.products.minimum-count:150}") int minimumCount) {
        return args -> seedCatalog(minimumCount);
    }

    @Transactional
    void seedCatalog(int minimumCount) {
        long activeProducts = productsRepository.countByIsActiveTrue();
        if (activeProducts >= minimumCount) {
            return;
        }

        List<Brands> brands = ensureBrands();
        int productsToCreate = (int) (minimumCount - activeProducts);

        for (int index = 0; index < productsToCreate; index += 1) {
            int catalogIndex = (int) activeProducts + index;
            Products product = Products.builder()
                    .brand(brands.get(catalogIndex % brands.size()))
                    .name(productName(catalogIndex))
                    .description("A versatile sneaker with everyday cushioning, durable materials, and a clean streetwear profile.")
                    .price(BigDecimal.valueOf(5499L + ((catalogIndex % 18) * 500L)))
                    .currency("INR")
                    .category(catalogIndex % 4 == 0 ? "Running" : catalogIndex % 4 == 1 ? "Lifestyle" : catalogIndex % 4 == 2 ? "Training" : "Skate")
                    .imageUrl(IMAGE_URLS[catalogIndex % IMAGE_URLS.length])
                    .sizes(SIZE_SETS.get(catalogIndex % SIZE_SETS.size()))
                    .colors(copyColors(COLOR_SETS.get(catalogIndex % COLOR_SETS.size())))
                    .stockStatus(catalogIndex % 7 == 0 ? "Only a few left" : catalogIndex % 3 == 0 ? "Selling fast" : "In stock")
                    .isActive(true)
                    .build();

            productsRepository.save(product);
        }
    }

    private List<Brands> ensureBrands() {
        return List.of(BRAND_NAMES).stream()
                .map(name -> brandsRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> brandsRepository.save(Brands.builder().name(name).build())))
                .toList();
    }

    private static List<ProductColor> copyColors(List<ProductColor> colors) {
        return colors.stream()
                .map(color -> new ProductColor(color.getName(), color.getValue()))
                .toList();
    }

    private static String productName(int catalogIndex) {
        String baseName = MODEL_NAMES[catalogIndex % MODEL_NAMES.length];
        int dropNumber = (catalogIndex / MODEL_NAMES.length) + 1;

        return dropNumber == 1 ? baseName : baseName + " Drop " + dropNumber;
    }
}
