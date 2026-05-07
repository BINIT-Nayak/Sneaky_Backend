package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.cart.CartItemDTO;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.CartRepository;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ProductsRepository productsRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addToCartCreatesNewCartItemWithProductSnapshot() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemDTO created = cartService.addToCart(user.getUserId(), product.getProductId(), 2);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getProduct()).isEqualTo(product);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(12999));
        assertThat(captor.getValue().getCurrency()).isEqualTo("INR");
        assertThat(created.getItemTotal()).isEqualByComparingTo(BigDecimal.valueOf(25998));
    }

    @Test
    void addToCartDefaultsQuantityToOne() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemDTO created = cartService.addToCart(user.getUserId(), product.getProductId(), null);

        assertThat(created.getQuantity()).isEqualTo(1);
    }

    @Test
    void addToCartIncrementsExistingCartItemAndRefreshesOrderingTimestamp() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));
        Cart existing = cart(user, product, 2);
        LocalDateTime previousCreatedAt = LocalDateTime.now().minusDays(1);
        existing.setCreatedAt(previousCreatedAt);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));
        when(cartRepository.save(existing)).thenReturn(existing);

        CartItemDTO updated = cartService.addToCart(user.getUserId(), product.getProductId(), 3);

        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(existing.getCreatedAt()).isAfter(previousCreatedAt);
        assertThat(updated.getQuantity()).isEqualTo(5);
        verify(cartRepository).save(existing);
    }

    @Test
    void getCartMapsProductBrandAndTotals() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));
        Cart cart = cart(user, product, 2);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of(cart));

        assertThat(cartService.getCart(user.getUserId())).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(product.getProductId());
            assertThat(item.getName()).isEqualTo("Air Max");
            assertThat(item.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(12999));
            assertThat(item.getCurrency()).isEqualTo("INR");
            assertThat(item.getImageUrl()).isEqualTo("image.jpg");
            assertThat(item.getBrandName()).isEqualTo("Nike");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getItemTotal()).isEqualByComparingTo(BigDecimal.valueOf(25998));
        });
    }

    @Test
    void updateQuantitySetsQuantityForExistingCartItem() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));
        Cart cart = cart(user, product, 1);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);

        CartItemDTO updated = cartService.updateQuantity(user.getUserId(), product.getProductId(), 4);

        assertThat(cart.getQuantity()).isEqualTo(4);
        assertThat(updated.getItemTotal()).isEqualByComparingTo(BigDecimal.valueOf(51996));
    }

    @Test
    void removeFromCartDeletesExistingCartItem() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));
        Cart cart = cart(user, product, 1);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(cart));

        cartService.removeFromCart(user.getUserId(), product.getProductId());

        verify(cartRepository).delete(cart);
    }

    @Test
    void clearCartDeletesAllItemsForUser() {
        Users user = user(UUID.randomUUID());

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

        cartService.clearCart(user.getUserId());

        verify(cartRepository).deleteByUser(user);
    }

    @Test
    void updateQuantityRejectsInvalidQuantity() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.updateQuantity(user.getUserId(), product.getProductId(), 0))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCartRejectsUnknownUserBeforeLookingUpProduct() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(userId, productId, 1))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(productsRepository, never()).findById(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCartRejectsMissingCartItem() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(user.getUserId(), product.getProductId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).delete(any());
    }

    private static Users user(UUID id) {
        Users user = new Users();
        user.setUserId(id);
        user.setName("Sneaky User");
        user.setEmail("user@sneaky.test");
        user.setPassword("secret");
        return user;
    }

    private static Products product(UUID id, Brands brand) {
        Products product = new Products();
        product.setProductId(id);
        product.setName("Air Max");
        product.setPrice(BigDecimal.valueOf(12999));
        product.setCurrency("INR");
        product.setImageUrl("image.jpg");
        product.setBrand(brand);
        return product;
    }

    private static Brands brand(String name) {
        return Brands.builder().id(UUID.randomUUID()).name(name).build();
    }

    private static Cart cart(Users user, Products product, int quantity) {
        return Cart.builder()
                .cartId(1L)
                .user(user)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .currency(product.getCurrency())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
