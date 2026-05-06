package com.sneaky.sneaky.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class BrandsRepositoryTest {

    @Test
    void existsByNameIgnoreCaseHasExpectedSpringDataContract() throws Exception {
        Method method = BrandsRepository.class.getMethod("existsByNameIgnoreCase", String.class);

        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }
}
