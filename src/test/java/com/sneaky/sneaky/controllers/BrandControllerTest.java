package com.sneaky.sneaky.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sneaky.sneaky.dto.brand.BrandDTO;
import com.sneaky.sneaky.dto.brand.CreateBrandRequestDTO;
import com.sneaky.sneaky.dto.brand.UpdateBrandRequestDTO;
import com.sneaky.sneaky.services.BrandService;

class BrandControllerTest {

    private final BrandService brandService = org.mockito.Mockito.mock(BrandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new BrandController(brandService))
                .setValidator(validator)
                .build();
    }

    @Test
    void createBrandReturnsCreatedBrand() throws Exception {
        UUID brandId = UUID.randomUUID();
        CreateBrandRequestDTO request = new CreateBrandRequestDTO();
        request.setName("Nike");

        when(brandService.createBrand(any(CreateBrandRequestDTO.class)))
                .thenReturn(new BrandDTO(brandId, "Nike"));

        mockMvc.perform(post("/api/brands")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(brandId.toString()))
                .andExpect(jsonPath("$.name").value("Nike"));
    }

    @Test
    void createBrandRejectsBlankNameBeforeServiceCall() throws Exception {
        CreateBrandRequestDTO request = new CreateBrandRequestDTO();
        request.setName("");

        mockMvc.perform(post("/api/brands")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(brandService);
    }

    @Test
    void getUpdateAndDeleteBrandDelegateToService() throws Exception {
        UUID brandId = UUID.randomUUID();
        UpdateBrandRequestDTO request = new UpdateBrandRequestDTO();
        request.setName("Adidas");

        when(brandService.getAllBrands()).thenReturn(List.of(new BrandDTO(brandId, "Nike")));
        when(brandService.getBrandById(brandId)).thenReturn(new BrandDTO(brandId, "Nike"));
        when(brandService.updateBrand(eq(brandId), any(UpdateBrandRequestDTO.class)))
                .thenReturn(new BrandDTO(brandId, "Adidas"));

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nike"));

        mockMvc.perform(get("/api/brands/{id}", brandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(brandId.toString()));

        mockMvc.perform(put("/api/brands/{id}", brandId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Adidas"));

        mockMvc.perform(delete("/api/brands/{id}", brandId))
                .andExpect(status().isNoContent());

        verify(brandService).deleteBrand(brandId);
    }
}
