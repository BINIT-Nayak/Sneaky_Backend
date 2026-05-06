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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService)).build();
    }

    @Test
    void getProductsAndSingleProductDelegateToService() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductDTO product = productDto(productId, "Air Max");

        when(productService.getActiveProducts()).thenReturn(List.of(product));
        when(productService.getProductById(productId)).thenReturn(product);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].brand").value("Nike"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Air Max"));
    }

    @Test
    void createUpdatePatchAndDeleteProductDelegateToService() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateProductRequestDTO createRequest = new CreateProductRequestDTO();
        createRequest.setName("Air Max");
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

    private static ProductDTO productDto(UUID id, String name) {
        return new ProductDTO(
                id,
                name,
                BigDecimal.valueOf(12999),
                "image.jpg",
                "Comfortable",
                "Nike",
                "Sneakers");
    }
}
