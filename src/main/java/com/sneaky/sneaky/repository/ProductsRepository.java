package com.sneaky.sneaky.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Products;

@Repository
public interface ProductsRepository extends JpaRepository<Products, UUID> {
    @EntityGraph(attributePaths = "brand")
    List<Products> findByIsActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "brand")
    List<Products> findByProductIdIn(List<UUID> productIds);

    long countByIsActiveTrue();
}
