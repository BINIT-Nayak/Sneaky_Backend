package com.sneaky.sneaky.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.analytics.UserActivityEventType;
import com.sneaky.sneaky.dto.cart.CartItemDTO;
import com.sneaky.sneaky.dto.wishlist.WishlistItemDTO;
import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.entity.WishList;
import com.sneaky.sneaky.repository.CartRepository;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.repository.WishListRepository;
import com.sneaky.sneaky.services.analytics.ActivityEventPublisher;
import com.sneaky.sneaky.services.analytics.UserActivityEventFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishListRepository wishListRepository;
    private final CartRepository cartRepository;
    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;
    private final ActivityEventPublisher activityEventPublisher;
    private final UserActivityEventFactory activityEventFactory;

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
            publishWishlistEvent(UserActivityEventType.WISHLIST_ADDED, userId, productId);
            return;
        }

        WishList wishlist = WishList.builder()
                .user(user)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        wishListRepository.save(wishlist);
        publishWishlistEvent(UserActivityEventType.WISHLIST_ADDED, userId, productId);
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
                            brandName,
                            p.getCategory(),
                            ProductService.resolveSizes(p.getSizes()),
                            ProductService.toColorDtos(p.getColors()),
                            ProductService.resolveStockStatus(p.getStockStatus()));
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
        publishWishlistEvent(UserActivityEventType.WISHLIST_REMOVED, userId, productId);
    }

    @Transactional
    public CartItemDTO moveToCart(UUID userId, UUID productId) {
        WishList wishlist = wishListRepository.findByUserIdAndProductIdWithProductAndBrand(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Users user = wishlist.getUser();
        Products product = wishlist.getProduct();

        Cart cart = cartRepository.findByUserUserIdAndProductProductId(userId, productId)
                .map(existingCart -> {
                    existingCart.setQuantity(existingCart.getQuantity() + 1);
                    existingCart.setPrice(product.getPrice());
                    existingCart.setCurrency(product.getCurrency());
                    existingCart.setCreatedAt(LocalDateTime.now());
                    existingCart.setReminderSentAt(null);
                    return existingCart;
                })
                .orElseGet(() -> Cart.builder()
                        .user(user)
                        .product(product)
                        .quantity(1)
                        .price(product.getPrice())
                        .currency(product.getCurrency())
                        .createdAt(LocalDateTime.now())
                        .build());

        Cart savedCart = cartRepository.save(cart);
        wishListRepository.delete(wishlist);
        publishCartEvent(UserActivityEventType.CART_ADDED, userId, productId);
        publishWishlistEvent(UserActivityEventType.WISHLIST_REMOVED, userId, productId);

        return toCartDto(savedCart, product);
    }

    @Transactional
    public void clearWishlist(UUID userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        wishListRepository.deleteByUser(user);
    }

    private CartItemDTO toCartDto(Cart cart) {
        return toCartDto(cart, cart.getProduct());
    }

    private CartItemDTO toCartDto(Cart cart, Products product) {
        String brandName = product.getBrand() == null ? "" : product.getBrand().getName();
        BigDecimal price = cart.getPrice() == null ? product.getPrice() : cart.getPrice();
        String currency = cart.getCurrency() == null ? product.getCurrency() : cart.getCurrency();

        return new CartItemDTO(
                product.getProductId(),
                product.getName(),
                price,
                currency,
                product.getImageUrl(),
                brandName,
                product.getCategory(),
                ProductService.resolveMerchantName(product.getMerchantName()),
                ProductService.resolveMerchantUrl(product.getMerchantUrl()),
                cart.getQuantity(),
                price.multiply(BigDecimal.valueOf(cart.getQuantity())),
                ProductService.resolveSizes(product.getSizes()),
                ProductService.toColorDtos(product.getColors()),
                ProductService.resolveStockStatus(product.getStockStatus()));
    }

    private void publishCartEvent(UserActivityEventType eventType, UUID userId, UUID productId) {
        activityEventPublisher.publish(activityEventFactory.create(eventType, userId, productId, 1));
    }

    private void publishWishlistEvent(UserActivityEventType eventType, UUID userId, UUID productId) {
        activityEventPublisher.publish(activityEventFactory.create(eventType, userId, productId, null));
    }
}
