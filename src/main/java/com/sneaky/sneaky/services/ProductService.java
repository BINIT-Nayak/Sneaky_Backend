package com.sneaky.sneaky.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.analytics.UserActivityEventType;
import com.sneaky.sneaky.dto.product.*;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.ProductColor;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.BrandsRepository;
import com.sneaky.sneaky.services.analytics.ActivityEventPublisher;
import com.sneaky.sneaky.services.analytics.UserActivityEventFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final List<String> DEFAULT_SIZES = List.of("UK 6", "UK 7", "UK 8", "UK 9", "UK 10");
    private static final List<ProductColorDTO> DEFAULT_COLORS = List.of(
            new ProductColorDTO("Black", "#17151d"),
            new ProductColorDTO("Ivory", "#eee4cf"),
            new ProductColorDTO("Clay", "#c27a58"));
    private static final String DEFAULT_STOCK_STATUS = "In stock";
    private static final String DEFAULT_MERCHANT_NAME = "Sneaky Partner";
    private static final String DEFAULT_MERCHANT_URL = "https://www.google.com/";
    private static final String APPROVED_STATUS = "APPROVED";

    private final ProductsRepository productsRepository;
    private final BrandsRepository brandsRepository;
    private final ActivityEventPublisher activityEventPublisher;
    private final UserActivityEventFactory activityEventFactory;
    private final ProductRecommendationService productRecommendationService;

    @Transactional(readOnly = true)
    public List<ProductDTO> getActiveProducts() {
        return productsRepository.findByIsActiveTrueAndStatusOrderByCreatedAtDesc(APPROVED_STATUS)
                .stream()
                .map(product -> toDTO(product, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getRecommendedProducts(UUID userId) {
        ProductRecommendationService.RecommendationResult recommendationResult =
                productRecommendationService.getRecommendedProducts(userId);

        return recommendationResult.products()
                .stream()
                .map(product -> toDTO(product, recommendationResult.personalized()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(UUID id) {
        Products product = getPublicProductEntity(id);
        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(UUID id, UUID viewerUserId) {
        Products product = getPublicProductEntity(id);
        publishProductEvent(UserActivityEventType.PRODUCT_VIEWED, viewerUserId, id, null);
        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public void recordProductPass(UUID id, UUID viewerUserId) {
        getProductEntity(id);
        publishProductEvent(UserActivityEventType.PRODUCT_PASSED, viewerUserId, id, null);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByIdsPreservingOrder(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        List<Products> products = productsRepository.findAllById(productIds);

        return productIds.stream()
                .flatMap(productId -> products.stream()
                        .filter(product -> productId.equals(product.getProductId()))
                        .findFirst()
                        .stream())
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ProductDTO createProduct(CreateProductRequestDTO request) {

        Products product = new Products();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());
        product.setMerchantName(resolveMerchantName(request.getMerchantName()));
        product.setMerchantUrl(resolveMerchantUrl(request.getMerchantUrl()));
        product.setSizes(resolveSizes(request.getSizes()));
        product.setColors(toProductColors(request.getColors()));
        product.setStockStatus(resolveStockStatus(request.getStockStatus()));
        product.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        product.setUpdatedAt(LocalDateTime.now());

        // brand mapping
        if (request.getBrandId() != null) {
            Brands brand = brandsRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
            product.setBrand(brand);
        }

        Products saved = productsRepository.save(product);

        return toDTO(saved);
    }

    @Transactional
    public ProductDTO updateProduct(UUID id, UpdateProductRequestDTO request) {

        Products product = getProductEntity(id);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());
        product.setMerchantName(resolveMerchantName(request.getMerchantName()));
        product.setMerchantUrl(resolveMerchantUrl(request.getMerchantUrl()));
        product.setSizes(resolveSizes(request.getSizes()));
        product.setColors(toProductColors(request.getColors()));
        product.setStockStatus(resolveStockStatus(request.getStockStatus()));
        product.setIsActive(request.getIsActive());
        if (request.getBrandId() != null) {
            Brands brand = brandsRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
            product.setBrand(brand);
        }

        product.setUpdatedAt(LocalDateTime.now());

        return toDTO(productsRepository.save(product));
    }

    @Transactional
    public ProductDTO patchProduct(UUID id, UpdateProductRequestDTO request) {

        Products product = getProductEntity(id);

        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getPrice() != null)
            product.setPrice(request.getPrice());
        if (request.getImageUrl() != null)
            product.setImageUrl(request.getImageUrl());
        if (request.getCategory() != null)
            product.setCategory(request.getCategory());
        if (request.getMerchantName() != null)
            product.setMerchantName(resolveMerchantName(request.getMerchantName()));
        if (request.getMerchantUrl() != null)
            product.setMerchantUrl(resolveMerchantUrl(request.getMerchantUrl()));
        if (request.getSizes() != null)
            product.setSizes(resolveSizes(request.getSizes()));
        if (request.getColors() != null)
            product.setColors(toProductColors(request.getColors()));
        if (request.getStockStatus() != null)
            product.setStockStatus(resolveStockStatus(request.getStockStatus()));
        if (request.getIsActive() != null)
            product.setIsActive(request.getIsActive());

        if (request.getBrandId() != null) {
            Brands brand = brandsRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
            product.setBrand(brand);
        }

        product.setUpdatedAt(LocalDateTime.now());

        return toDTO(productsRepository.save(product));
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Products product = getProductEntity(id);
        product.setIsActive(false); // soft delete
        product.setUpdatedAt(LocalDateTime.now());
        productsRepository.save(product);
    }

    // helper
    private Products getProductEntity(UUID id) {
        return productsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Products getPublicProductEntity(UUID id) {
        Products product = getProductEntity(id);

        if (!Boolean.TRUE.equals(product.getIsActive()) || !APPROVED_STATUS.equalsIgnoreCase(product.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        return product;
    }

    private ProductDTO toDTO(Products product) {
        return toDTO(product, false);
    }

    private ProductDTO toDTO(Products product, boolean recommended) {
        Brands brand = product.getBrand();

        return new ProductDTO(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDescription(),
                brand == null ? "" : brand.getName(),
                product.getCategory(),
                resolveMerchantName(product.getMerchantName()),
                resolveMerchantUrl(product.getMerchantUrl()),
                resolveSizes(product.getSizes()),
                resolveColors(toColorDtos(product.getColors())),
                resolveStockStatus(product.getStockStatus()),
                recommended);
    }

    public static List<String> resolveSizes(List<String> sizes) {
        return sizes == null || sizes.isEmpty() ? DEFAULT_SIZES : List.copyOf(sizes);
    }

    public static List<ProductColorDTO> resolveColors(List<ProductColorDTO> colors) {
        return colors == null || colors.isEmpty() ? DEFAULT_COLORS : List.copyOf(colors);
    }

    public static String resolveStockStatus(String stockStatus) {
        return stockStatus == null || stockStatus.isBlank() ? DEFAULT_STOCK_STATUS : stockStatus;
    }

    public static String resolveMerchantName(String merchantName) {
        return merchantName == null || merchantName.isBlank() ? DEFAULT_MERCHANT_NAME : merchantName;
    }

    public static String resolveMerchantUrl(String merchantUrl) {
        return merchantUrl == null || merchantUrl.isBlank() ? DEFAULT_MERCHANT_URL : merchantUrl;
    }

    public static List<ProductColor> toProductColors(List<ProductColorDTO> colors) {
        return resolveColors(colors).stream()
                .map(color -> new ProductColor(color.getName(), color.getValue()))
                .toList();
    }

    public static List<ProductColorDTO> toColorDtos(List<ProductColor> colors) {
        if (colors == null || colors.isEmpty()) {
            return DEFAULT_COLORS;
        }

        return colors.stream()
                .map(color -> new ProductColorDTO(color.getName(), color.getValue()))
                .toList();
    }

    private void publishProductEvent(
            UserActivityEventType eventType,
            UUID userId,
            UUID productId,
            Integer quantity) {
        activityEventPublisher.publish(activityEventFactory.create(eventType, userId, productId, quantity));
    }
}
