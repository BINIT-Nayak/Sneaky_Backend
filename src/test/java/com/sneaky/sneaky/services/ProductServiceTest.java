package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.product.CreateProductRequestDTO;
import com.sneaky.sneaky.dto.product.ProductDTO;
import com.sneaky.sneaky.dto.product.UpdateProductRequestDTO;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.repository.BrandsRepository;
import com.sneaky.sneaky.repository.ProductsRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private BrandsRepository brandsRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getActiveProductsMapsBrandNameAndImageUrl() {
        UUID productId = UUID.randomUUID();
        Products product = product(productId, brand(UUID.randomUUID(), "Nike"));

        when(productsRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(product));

        assertThat(productService.getActiveProducts()).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(productId);
            assertThat(dto.getName()).isEqualTo("Air Max");
            assertThat(dto.getBrand()).isEqualTo("Nike");
            assertThat(dto.getImage()).isEqualTo("image.jpg");
        });
    }

    @Test
    void createProductUsesBrandWhenBrandIdIsPresent() {
        UUID brandId = UUID.randomUUID();
        Brands brand = brand(brandId, "Nike");
        CreateProductRequestDTO request = new CreateProductRequestDTO();
        request.setName("Air Max");
        request.setDescription("Comfortable");
        request.setPrice(BigDecimal.valueOf(12999));
        request.setImageUrl("image.jpg");
        request.setCategory("Sneakers");
        request.setBrandId(brandId);

        when(brandsRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(productsRepository.save(any(Products.class))).thenAnswer(invocation -> {
            Products product = invocation.getArgument(0);
            product.setProductId(UUID.randomUUID());
            return product;
        });

        ProductDTO created = productService.createProduct(request);

        ArgumentCaptor<Products> captor = ArgumentCaptor.forClass(Products.class);
        verify(productsRepository).save(captor.capture());
        assertThat(captor.getValue().getBrand()).isEqualTo(brand);
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(created.getBrand()).isEqualTo("Nike");
    }

    @Test
    void createProductRejectsMissingBrand() {
        UUID brandId = UUID.randomUUID();
        CreateProductRequestDTO request = new CreateProductRequestDTO();
        request.setBrandId(brandId);

        when(brandsRepository.findById(brandId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void patchProductUpdatesOnlyProvidedFields() {
        UUID productId = UUID.randomUUID();
        Products product = product(productId, null);
        UpdateProductRequestDTO request = new UpdateProductRequestDTO();
        request.setName("Updated");
        request.setIsActive(false);

        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productsRepository.save(product)).thenReturn(product);

        ProductDTO patched = productService.patchProduct(productId, request);

        assertThat(patched.getName()).isEqualTo("Updated");
        assertThat(product.getDescription()).isEqualTo("Comfortable");
        assertThat(product.getIsActive()).isFalse();
    }

    @Test
    void deleteProductSoftDeletesProduct() {
        UUID productId = UUID.randomUUID();
        Products product = product(productId, null);

        when(productsRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        assertThat(product.getIsActive()).isFalse();
        verify(productsRepository).save(product);
    }

    private static Products product(UUID id, Brands brand) {
        Products product = new Products();
        product.setProductId(id);
        product.setName("Air Max");
        product.setDescription("Comfortable");
        product.setPrice(BigDecimal.valueOf(12999));
        product.setImageUrl("image.jpg");
        product.setCategory("Sneakers");
        product.setIsActive(true);
        product.setBrand(brand);
        return product;
    }

    private static Brands brand(UUID id, String name) {
        return Brands.builder().id(id).name(name).build();
    }
}
