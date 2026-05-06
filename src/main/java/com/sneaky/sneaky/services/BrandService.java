package com.sneaky.sneaky.services;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.brand.*;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.repository.BrandsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandsRepository brandsRepository;

    @Transactional
    public BrandDTO createBrand(CreateBrandRequestDTO request) {

        String name = request.getName().trim();

        if (brandsRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand already exists");
        }

        Brands brand = new Brands();
        brand.setName(name);

        return toDTO(brandsRepository.save(brand));
    }

    @Transactional(readOnly = true)
    public List<BrandDTO> getAllBrands() {
        return brandsRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandDTO getBrandById(UUID id) {
        return toDTO(getBrandEntity(id));
    }

    @Transactional
    public BrandDTO updateBrand(UUID id, UpdateBrandRequestDTO request) {

        Brands brand = getBrandEntity(id);
        String newName = request.getName().trim();

        if (!brand.getName().equalsIgnoreCase(newName) &&
                brandsRepository.existsByNameIgnoreCase(newName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand already exists");
        }

        brand.setName(newName);

        return toDTO(brandsRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(UUID id) {
        Brands brand = getBrandEntity(id);
        brandsRepository.delete(brand);
    }

    // helper
    private Brands getBrandEntity(UUID id) {
        return brandsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
    }

    private BrandDTO toDTO(Brands brand) {
        return new BrandDTO(brand.getId(), brand.getName());
    }
}