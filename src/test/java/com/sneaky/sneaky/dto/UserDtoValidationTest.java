package com.sneaky.sneaky.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sneaky.sneaky.dto.user.CreateUserRequestDTO;
import com.sneaky.sneaky.dto.user.UpdateUserRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UserDtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void createUserDefaultsToNonGuestAndAcceptsStrongPassword() {
        CreateUserRequestDTO request = createUserRequest("Mina", "mina@example.com", "Secret@123");

        Set<ConstraintViolation<CreateUserRequestDTO>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.getIsGuest()).isFalse();
    }

    @Test
    void createUserRejectsInvalidEmailAndWeakPassword() {
        CreateUserRequestDTO request = createUserRequest("", "bad-email", "secret");

        Set<ConstraintViolation<CreateUserRequestDTO>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "Name is required",
                        "Invalid email format",
                        "Password must be at least 8 characters",
                        "Password must include letters, numbers, and a special character");
    }

    @Test
    void updateUserAllowsOmittedPasswordButValidatesProvidedPasswordAndEmail() {
        UpdateUserRequestDTO emptyPatch = new UpdateUserRequestDTO();
        UpdateUserRequestDTO invalidPatch = new UpdateUserRequestDTO();
        invalidPatch.setEmail("bad-email");
        invalidPatch.setPassword("password1");

        assertThat(validator.validate(emptyPatch)).isEmpty();
        assertThat(validator.validate(invalidPatch))
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "Invalid email format",
                        "Password must include letters, numbers, and a special character");
    }

    private static CreateUserRequestDTO createUserRequest(String name, String email, String password) {
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
