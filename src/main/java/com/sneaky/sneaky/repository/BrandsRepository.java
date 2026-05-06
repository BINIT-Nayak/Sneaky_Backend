package com.sneaky.sneaky.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Brands;

@Repository
public interface BrandsRepository extends JpaRepository<Brands, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
