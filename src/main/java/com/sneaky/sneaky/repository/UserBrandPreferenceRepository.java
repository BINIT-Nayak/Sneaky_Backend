package com.sneaky.sneaky.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.UserBrandPreference;
import com.sneaky.sneaky.entity.Users;

@Repository
public interface UserBrandPreferenceRepository extends JpaRepository<UserBrandPreference, Long> {
    Optional<UserBrandPreference> findByUserAndBrand(Users user, Brands brand);

    List<UserBrandPreference> findByUserUserId(UUID userId);
}
