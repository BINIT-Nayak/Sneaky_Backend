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
public class CartService {

    private final CartRepository cartRepository;
    private final WishListRepository wishListRepository;
    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;
    private final ActivityEventPublisher activityEventPublisher;
    private final UserActivityEventFactory activityEventFactory;

    @Transactional
    public CartItemDTO addToCart(UUID userId, UUID productId, Integer requestedQuantity) {
        Users user = getUser(userId);
        Products product = getProduct(productId);
        int quantity = normalizeQuantity(requestedQuantity);

        Cart cart = cartRepository.findByUserAndProduct(user, product)
                .map(existingCart -> {
                    existingCart.setQuantity(existingCart.getQuantity() + quantity);
                    existingCart.setPrice(product.getPrice());
                    existingCart.setCurrency(product.getCurrency());
                    existingCart.setCreatedAt(LocalDateTime.now());
                    return existingCart;
                })
                .orElseGet(() -> Cart.builder()
                        .user(user)
                        .product(product)
                        .quantity(quantity)
                        .price(product.getPrice())
                        .currency(product.getCurrency())
                        .createdAt(LocalDateTime.now())
                        .build());

        Cart saved = cartRepository.save(cart);
        publishCartEvent(UserActivityEventType.CART_ADDED, userId, productId, quantity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCart(UUID userId) {
        Users user = getUser(userId);

        return cartRepository.findByUserWithProductAndBrand(user)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CartItemDTO updateQuantity(UUID userId, UUID productId, Integer requestedQuantity) {
        Users user = getUser(userId);
        Products product = getProduct(productId);
        int quantity = normalizeQuantity(requestedQuantity);

        Cart cart = cartRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        cart.setQuantity(quantity);
        Cart saved = cartRepository.save(cart);
        publishCartEvent(UserActivityEventType.CART_QUANTITY_UPDATED, userId, productId, quantity);
        return toDto(saved);
    }

    @Transactional
    public void removeFromCart(UUID userId, UUID productId) {
        Users user = getUser(userId);
        Products product = getProduct(productId);

        Cart cart = cartRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        cartRepository.delete(cart);
        publishCartEvent(UserActivityEventType.CART_REMOVED, userId, productId, null);
    }

    @Transactional
    public WishlistItemDTO moveToWishlist(UUID userId, UUID productId) {
        Cart cart = cartRepository.findByUserIdAndProductIdWithProductAndBrand(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Users user = cart.getUser();
        Products product = cart.getProduct();

        WishList wishlist = wishListRepository.findByUserAndProduct(user, product)
                .map(existingWishlist -> {
                    existingWishlist.setCreatedAt(LocalDateTime.now());
                    return existingWishlist;
                })
                .orElseGet(() -> WishList.builder()
                        .user(user)
                        .product(product)
                        .createdAt(LocalDateTime.now())
                        .build());

        WishList savedWishlist = wishListRepository.save(wishlist);
        cartRepository.delete(cart);
        publishWishlistEvent(UserActivityEventType.WISHLIST_ADDED, userId, productId);
        publishCartEvent(UserActivityEventType.CART_REMOVED, userId, productId, null);

        return toWishlistDto(savedWishlist);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Users user = getUser(userId);
        cartRepository.deleteByUser(user);
    }

    private Users getUser(UUID userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Products getProduct(UUID productId) {
        return productsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private static int normalizeQuantity(Integer quantity) {
        if (quantity == null) {
            return 1;
        }

        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        return quantity;
    }

    private CartItemDTO toDto(Cart cart) {
        Products product = cart.getProduct();
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

    private WishlistItemDTO toWishlistDto(WishList wishlist) {
        Products product = wishlist.getProduct();
        String brandName = product.getBrand() == null ? "" : product.getBrand().getName();

        return new WishlistItemDTO(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                brandName,
                product.getCategory(),
                ProductService.resolveSizes(product.getSizes()),
                ProductService.toColorDtos(product.getColors()),
                ProductService.resolveStockStatus(product.getStockStatus()));
    }

    private void publishCartEvent(UserActivityEventType eventType, UUID userId, UUID productId, Integer quantity) {
        activityEventPublisher.publish(activityEventFactory.create(eventType, userId, productId, quantity));
    }

    private void publishWishlistEvent(UserActivityEventType eventType, UUID userId, UUID productId) {
        activityEventPublisher.publish(activityEventFactory.create(eventType, userId, productId, null));
    }
}
