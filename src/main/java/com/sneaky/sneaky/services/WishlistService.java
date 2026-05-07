package com.sneaky.sneaky.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.wishlist.WishlistItemDTO;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.entity.WishList;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.repository.WishListRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishListRepository wishListRepository;
    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;

    @Transactional
    public void addToWishlist(UUID userId, UUID productId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        WishList existing = wishListRepository.findByUserAndProduct(user, product)
                .orElse(null);

        if (existing != null) {
            existing.setCreatedAt(LocalDateTime.now());
            wishListRepository.save(existing);
            return;
        }

        WishList wishlist = WishList.builder()
                .user(user)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        wishListRepository.save(wishlist);
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getWishlist(UUID userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return wishListRepository.findByUserWithProductAndBrand(user)
                .stream()
                .map(w -> {
                    Products p = w.getProduct();
                    String brandName = p.getBrand() == null ? "" : p.getBrand().getName();

                    return new WishlistItemDTO(
                            p.getProductId(),
                            p.getName(),
                            p.getPrice(),
                            p.getImageUrl(),
                            brandName);
                })
                .toList();
    }

    @Transactional
    public void removeFromWishlist(UUID userId, UUID productId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        WishList wishlist = wishListRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        wishListRepository.delete(wishlist);
    }
}