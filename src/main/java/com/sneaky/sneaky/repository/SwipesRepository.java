package com.sneaky.sneaky.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Swipe;

@Repository
public interface SwipesRepository extends JpaRepository<Swipe, UUID> {
    long countByCreatedAtAfter(LocalDateTime createdAt);
    long countByUserUserId(UUID userId);
}
