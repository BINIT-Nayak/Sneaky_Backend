package com.sneaky.sneaky.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sneaky.sneaky.entity.Products;

class ProductsRepositoryTest {

    @Test
    void extendsJpaRepositoryWithProductUuidId() {
        assertThat(JpaRepository.class).isAssignableFrom(ProductsRepository.class);
        assertThat(ProductsRepository.class.getGenericInterfaces()[0].getTypeName())
                .contains(Products.class.getName())
                .contains(UUID.class.getName());
    }

    @Test
    void findByIsActiveTrueOrderByCreatedAtDescHasExpectedSpringDataContract() throws Exception {
        Method method = ProductsRepository.class.getMethod("findByIsActiveTrueOrderByCreatedAtDesc");

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(Products.class.getName());
    }
}
