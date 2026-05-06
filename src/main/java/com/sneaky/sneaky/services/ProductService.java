package com.sneaky.sneaky.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sneaky.sneaky.dto.ProductDTO;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.repository.ProductsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductsRepository productsRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO> getActiveProducts() {
        return productsRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ProductDTO toDTO(Products product) {
        Brands brand = product.getBrand();

        return new ProductDTO(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDescription(),
                brand == null ? "" : brand.getName(),
                product.getCategory());
    }
}
