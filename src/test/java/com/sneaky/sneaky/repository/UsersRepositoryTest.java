package com.sneaky.sneaky.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sneaky.sneaky.entity.Users;

class UsersRepositoryTest {

    @Test
    void findByEmailHasExpectedSpringDataContract() throws Exception {
        Method method = UsersRepository.class.getMethod("findByEmail", String.class);

        assertThat(method.getReturnType()).isEqualTo(Optional.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(Users.class.getName());
    }
}
