package com.sneaky.sneaky.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.LogoutRequestDTO;
import com.sneaky.sneaky.dto.auth.RefreshRequestDTO;
import com.sneaky.sneaky.dto.brand.CreateBrandRequestDTO;
import com.sneaky.sneaky.dto.brand.UpdateBrandRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AuthAndBrandDtoValidationTest {

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
    void loginRequestRequiresValidEmailAndPassword() {
        LoginRequestDTO validRequest = new LoginRequestDTO();
        validRequest.setEmail("mina@example.com");
        validRequest.setPassword("Secret@123");

        LoginRequestDTO invalidRequest = new LoginRequestDTO();
        invalidRequest.setEmail("bad-email");
        invalidRequest.setPassword("");

        assertThat(validator.validate(validRequest)).isEmpty();
        assertThat(messagesFor(invalidRequest))
                .contains("Invalid email format", "Password is required");
    }

    @Test
    void refreshAndLogoutRequestsRequireRefreshToken() {
        RefreshRequestDTO refreshRequest = new RefreshRequestDTO();
        LogoutRequestDTO logoutRequest = new LogoutRequestDTO();

        assertThat(messagesFor(refreshRequest)).contains("Refresh token is required");
        assertThat(messagesFor(logoutRequest)).contains("Refresh token is required");

        refreshRequest.setRefreshToken("refresh-token");
        logoutRequest.setRefreshToken("refresh-token");

        assertThat(validator.validate(refreshRequest)).isEmpty();
        assertThat(validator.validate(logoutRequest)).isEmpty();
    }

    @Test
    void brandRequestsRequireNameForCreateAndUpdate() {
        CreateBrandRequestDTO createRequest = new CreateBrandRequestDTO();
        UpdateBrandRequestDTO updateRequest = new UpdateBrandRequestDTO();

        assertThat(messagesFor(createRequest)).contains("Brand name is required");
        assertThat(messagesFor(updateRequest)).contains("Brand name is required");

        createRequest.setName("Nike");
        updateRequest.setName("Adidas");

        assertThat(validator.validate(createRequest)).isEmpty();
        assertThat(validator.validate(updateRequest)).isEmpty();
    }

    private static <T> Set<String> messagesFor(T dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
