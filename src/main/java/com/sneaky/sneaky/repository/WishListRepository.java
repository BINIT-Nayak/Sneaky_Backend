package com.sneaky.sneaky.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.entity.WishList;

@Repository
public interface WishListRepository extends JpaRepository<WishList, Long> {
    List<WishList> findByUser(Users user);

    boolean existsByUserAndProduct(Users user, Products product);

    Optional<WishList> findByUserAndProduct(Users user, Products product);

    List<WishList> findByUserOrderByCreatedAtDesc(Users user);

    @Query("SELECT w FROM WishList w JOIN FETCH w.product p LEFT JOIN FETCH p.brand WHERE w.user = :user ORDER BY w.createdAt DESC")
    List<WishList> findByUserWithProductAndBrand(Users user);
}
