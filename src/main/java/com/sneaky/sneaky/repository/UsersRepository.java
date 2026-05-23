package com.sneaky.sneaky.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // NEW METHODS FOR ADMIN
    Page<Users> findByRole(String role, Pageable pageable);
    Page<Users> findByIsBanned(boolean isBanned, Pageable pageable);
    long countByRole(String role);
    long countByIsBanned(boolean isBanned);
}