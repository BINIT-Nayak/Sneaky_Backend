package com.sneaky.sneaky.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.UserCategoryPreference;
import com.sneaky.sneaky.entity.Users;

@Repository
public interface UserCategoryPreferenceRepository extends JpaRepository<UserCategoryPreference, Long> {
    Optional<UserCategoryPreference> findByUserAndCategoryIgnoreCase(Users user, String category);

    List<UserCategoryPreference> findByUserUserId(UUID userId);
}
