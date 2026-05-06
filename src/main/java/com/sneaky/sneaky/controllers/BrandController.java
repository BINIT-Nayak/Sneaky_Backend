package com.sneaky.sneaky.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.sneaky.sneaky.dto.brand.*;
import com.sneaky.sneaky.services.BrandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandDTO createBrand(@Valid @RequestBody CreateBrandRequestDTO request) {
        return brandService.createBrand(request);
    }

    @GetMapping
    public List<BrandDTO> getBrands() {
        return brandService.getAllBrands();
    }

    @GetMapping("/{id}")
    public BrandDTO getBrand(@PathVariable UUID id) {
        return brandService.getBrandById(id);
    }

    @PutMapping("/{id}")
    public BrandDTO updateBrand(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBrandRequestDTO request) {
        return brandService.updateBrand(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable UUID id) {
        brandService.deleteBrand(id);
    }
}