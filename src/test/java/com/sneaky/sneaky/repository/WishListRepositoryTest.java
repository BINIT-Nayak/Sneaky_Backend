package com.sneaky.sneaky.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.entity.WishList;

class WishListRepositoryTest {

    @Test
    void findByUserHasExpectedSpringDataContract() throws Exception {
        Method method = WishListRepository.class.getMethod("findByUser", Users.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(WishList.class.getName());
    }

    @Test
    void existsByUserAndProductHasExpectedSpringDataContract() throws Exception {
        Method method = WishListRepository.class.getMethod("existsByUserAndProduct", Users.class, Products.class);

        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void findByUserAndProductHasExpectedSpringDataContract() throws Exception {
        Method method = WishListRepository.class.getMethod("findByUserAndProduct", Users.class, Products.class);

        assertThat(method.getReturnType()).isEqualTo(Optional.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(WishList.class.getName());
    }

    @Test
    void findByUserOrderByCreatedAtDescHasExpectedSpringDataContract() throws Exception {
        Method method = WishListRepository.class.getMethod("findByUserOrderByCreatedAtDesc", Users.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(WishList.class.getName());
    }

    @Test
    void findByUserWithProductAndBrandUsesFetchQuery() throws Exception {
        Method method = WishListRepository.class.getMethod("findByUserWithProductAndBrand", Users.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(WishList.class.getName());
        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("JOIN FETCH w.product")
                .contains("LEFT JOIN FETCH p.brand")
                .contains("ORDER BY w.createdAt DESC");
    }
}
