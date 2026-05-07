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

import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.entity.WishList;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.repository.WishListRepository;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishListRepository wishListRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ProductsRepository productsRepository;

    @InjectMocks
    private WishlistService wishlistService;

    @Test
    void addToWishlistCreatesNewWishlistItem() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(wishListRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        wishlistService.addToWishlist(user.getUserId(), product.getProductId());

        ArgumentCaptor<WishList> captor = ArgumentCaptor.forClass(WishList.class);
        verify(wishListRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getProduct()).isEqualTo(product);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void addToWishlistRefreshesTimestampWhenItemAlreadyExists() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));
        WishList existing = wishlist(user, product);
        LocalDateTime previousCreatedAt = LocalDateTime.now().minusDays(2);
        existing.setCreatedAt(previousCreatedAt);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(wishListRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        wishlistService.addToWishlist(user.getUserId(), product.getProductId());

        assertThat(existing.getCreatedAt()).isAfter(previousCreatedAt);
        verify(wishListRepository).save(existing);
    }

    @Test
    void getWishlistMapsProductAndBrandFields() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of(wishlist(user, product)));

        assertThat(wishlistService.getWishlist(user.getUserId())).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(product.getProductId());
            assertThat(item.getName()).isEqualTo("Air Max");
            assertThat(item.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(12999));
            assertThat(item.getImageUrl()).isEqualTo("image.jpg");
            assertThat(item.getBrandName()).isEqualTo("Nike");
        });
    }

    @Test
    void getWishlistUsesEmptyBrandNameWhenProductHasNoBrand() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), null);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(wishListRepository.findByUserWithProductAndBrand(user)).thenReturn(List.of(wishlist(user, product)));

        assertThat(wishlistService.getWishlist(user.getUserId()))
                .singleElement()
                .extracting("brandName")
                .isEqualTo("");
    }

    @Test
    void removeFromWishlistDeletesExistingWishlistItem() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));
        WishList wishlist = wishlist(user, product);

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(wishListRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(wishlist));

        wishlistService.removeFromWishlist(user.getUserId(), product.getProductId());

        verify(wishListRepository).delete(wishlist);
    }

    @Test
    void removeFromWishlistRejectsMissingWishlistItem() {
        Users user = user(UUID.randomUUID());
        Products product = product(UUID.randomUUID(), brand("Nike"));

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(wishListRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.removeFromWishlist(user.getUserId(), product.getProductId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(wishListRepository, never()).delete(any());
    }

    @Test
    void addToWishlistRejectsUnknownUserBeforeLookingUpProduct() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist(userId, productId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(productsRepository, never()).findById(any());
        verify(wishListRepository, never()).save(any());
    }

    @Test
    void addToWishlistRejectsUnknownProduct() {
        Users user = user(UUID.randomUUID());
        UUID productId = UUID.randomUUID();

        when(usersRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(productsRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist(user.getUserId(), productId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(wishListRepository, never()).save(any());
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
        product.setImageUrl("image.jpg");
        product.setBrand(brand);
        return product;
    }

    private static Brands brand(String name) {
        return Brands.builder().id(UUID.randomUUID()).name(name).build();
    }

    private static WishList wishlist(Users user, Products product) {
        return WishList.builder()
                .wishlistId(1L)
                .user(user)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
