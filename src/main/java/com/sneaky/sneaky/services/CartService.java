package com.sneaky.sneaky.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.cart.CartItemDTO;
import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.CartRepository;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;

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

        return toDto(cartRepository.save(cart));
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
        return toDto(cartRepository.save(cart));
    }

    @Transactional
    public void removeFromCart(UUID userId, UUID productId) {
        Users user = getUser(userId);
        Products product = getProduct(productId);

        Cart cart = cartRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        cartRepository.delete(cart);
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
                cart.getQuantity(),
                price.multiply(BigDecimal.valueOf(cart.getQuantity())));
    }
}
