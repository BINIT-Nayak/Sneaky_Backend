package com.sneaky.sneaky.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class BrandsRepositoryTest {

    @Test
    void existsByNameIgnoreCaseHasExpectedSpringDataContract() throws Exception {
        Method method = BrandsRepository.class.getMethod("existsByNameIgnoreCase", String.class);

        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void findByNameIgnoreCaseHasExpectedSpringDataContract() throws Exception {
        Method method = BrandsRepository.class.getMethod("findByNameIgnoreCase", String.class);

        assertThat(method.getReturnType()).isEqualTo(Optional.class);
    }
}
