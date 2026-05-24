package com.sneaky.sneaky.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sneaky.sneaky.dto.product.CreateProductRequestDTO;
import com.sneaky.sneaky.dto.product.ProductDTO;
import com.sneaky.sneaky.dto.product.UpdateProductRequestDTO;
import com.sneaky.sneaky.services.ProductService;

class ProductControllerTest {

    private final ProductService productService = org.mockito.Mockito.mock(ProductService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setValidator(validator)
                .build();
    }

    @Test
    void getProductsAndSingleProductDelegateToService() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductDTO product = productDto(productId, "Air Max");

        when(productService.getActiveProducts()).thenReturn(List.of(product));
        when(productService.getProductById(productId, null)).thenReturn(product);
        when(productService.getRecommendedProducts(null)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].brand").value("Nike"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Air Max"));

        mockMvc.perform(get("/api/products/recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Air Max"));
    }

    @Test
    void recommendedProductsPassAuthenticatedUserToService() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductDTO product = productDto(productId, "Air Max");

        when(productService.getRecommendedProducts(userId)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products/recommended")
                        .principal(new UsernamePasswordAuthenticationToken(
                                userId.toString(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()));
    }

    @Test
    void createUpdatePatchAndDeleteProductDelegateToService() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateProductRequestDTO createRequest = new CreateProductRequestDTO();
        createRequest.setName("Air Max");
        createRequest.setPrice(BigDecimal.valueOf(12999));
        UpdateProductRequestDTO updateRequest = new UpdateProductRequestDTO();
        updateRequest.setName("Air Max 2");

        when(productService.createProduct(any(CreateProductRequestDTO.class)))
                .thenReturn(productDto(productId, "Air Max"));
        when(productService.updateProduct(eq(productId), any(UpdateProductRequestDTO.class)))
                .thenReturn(productDto(productId, "Air Max 2"));
        when(productService.patchProduct(eq(productId), any(UpdateProductRequestDTO.class)))
                .thenReturn(productDto(productId, "Air Max 2"));

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Air Max"));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Air Max 2"));

        mockMvc.perform(patch("/api/products/{id}", productId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Air Max 2"));

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(productId);
    }

    @Test
    void createProductRejectsInvalidPayloadBeforeServiceCall() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO();
        request.setName("");
        request.setPrice(BigDecimal.ZERO);

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, org.mockito.Mockito.never()).createProduct(any(CreateProductRequestDTO.class));
    }

    private static ProductDTO productDto(UUID id, String name) {
        return new ProductDTO(
                id,
                name,
                BigDecimal.valueOf(12999),
                "image.jpg",
                "Comfortable",
                "Nike",
                "Sneakers",
                "Nike",
                "https://www.nike.com/in/w/shoes-y7ok",
                List.of("UK 8", "UK 9"),
                List.of(new com.sneaky.sneaky.dto.product.ProductColorDTO("Black", "#17151d")),
                "Selling fast",
                false);
    }
}
