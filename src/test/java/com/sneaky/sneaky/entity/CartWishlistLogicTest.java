package com.sneaky.sneaky.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Table;

class CartWishlistLogicTest {

    @Test
    void cartAndProductDefaultsAreReadyForNewRows() {
        Cart cart = new Cart();
        Products product = new Products();

        assertThat(cart.getQuantity()).isEqualTo(1);
        assertThat(product.getCurrency()).isEqualTo("INR");
        assertThat(product.getIsActive()).isTrue();
    }

    @Test
    void cartAndWishlistPreventDuplicateUserProductRows() {
        Table cartTable = Cart.class.getAnnotation(Table.class);
        Table wishlistTable = WishList.class.getAnnotation(Table.class);

        assertThat(cartTable.uniqueConstraints()).singleElement().satisfies(uniqueConstraint ->
                assertThat(uniqueConstraint.columnNames()).containsExactly("user_id", "product_id"));
        assertThat(wishlistTable.uniqueConstraints()).singleElement().satisfies(uniqueConstraint ->
                assertThat(uniqueConstraint.columnNames()).containsExactly("user_id", "product_id"));
    }
}
