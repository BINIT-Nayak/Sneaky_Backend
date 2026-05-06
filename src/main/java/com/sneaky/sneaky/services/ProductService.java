package com.sneaky.sneaky.services;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.product.*;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.BrandsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductsRepository productsRepository;
    private final BrandsRepository brandsRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO> getActiveProducts() {
        return productsRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(UUID id) {
        Products product = getProductEntity(id);
        return toDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(CreateProductRequestDTO request) {

        Products product = new Products();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());
        product.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

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
        product.setIsActive(request.getIsActive());

        if (request.getBrandId() != null) {
            Brands brand = brandsRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
            product.setBrand(brand);
        }

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
        if (request.getIsActive() != null)
            product.setIsActive(request.getIsActive());

        if (request.getBrandId() != null) {
            Brands brand = brandsRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
            product.setBrand(brand);
        }

        return toDTO(productsRepository.save(product));
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Products product = getProductEntity(id);
        product.setIsActive(false); // soft delete
        productsRepository.save(product);
    }

    // helper
    private Products getProductEntity(UUID id) {
        return productsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
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
