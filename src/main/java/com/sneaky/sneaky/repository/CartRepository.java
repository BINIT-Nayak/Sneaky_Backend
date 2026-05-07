package com.sneaky.sneaky.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUser(Users user);

    Optional<Cart> findByUserAndProduct(Users user, Products product);

    void deleteByUser(Users user);

    @Query("SELECT c FROM Cart c JOIN FETCH c.product p LEFT JOIN FETCH p.brand WHERE c.user = :user ORDER BY c.createdAt DESC, c.cartId DESC")
    List<Cart> findByUserWithProductAndBrand(Users user);
}
