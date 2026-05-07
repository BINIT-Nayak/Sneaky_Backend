package com.sneaky.sneaky.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import com.sneaky.sneaky.entity.Cart;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;

class CartRepositoryTest {

    @Test
    void findByUserHasExpectedSpringDataContract() throws Exception {
        Method method = CartRepository.class.getMethod("findByUser", Users.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(Cart.class.getName());
    }

    @Test
    void findByUserAndProductHasExpectedSpringDataContract() throws Exception {
        Method method = CartRepository.class.getMethod("findByUserAndProduct", Users.class, Products.class);

        assertThat(method.getReturnType()).isEqualTo(Optional.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(Cart.class.getName());
    }

    @Test
    void deleteByUserHasExpectedSpringDataContract() throws Exception {
        Method method = CartRepository.class.getMethod("deleteByUser", Users.class);

        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void findByUserWithProductAndBrandUsesFetchQuery() throws Exception {
        Method method = CartRepository.class.getMethod("findByUserWithProductAndBrand", Users.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(Cart.class.getName());
        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("JOIN FETCH c.product")
                .contains("LEFT JOIN FETCH p.brand")
                .contains("ORDER BY c.createdAt DESC, c.cartId DESC");
    }
}
