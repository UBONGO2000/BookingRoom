package com.springbootlearning.learningspringboot.bookingroom.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterFormDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldNotReportPasswordErrors_whenPasswordsMatch() {
        Set<ConstraintViolation<RegisterFormDto>> violations = validator.validate(validForm());

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("password", "confirmPassword", "passwordConfirmed");
    }

    @Test
    void shouldReject_whenPasswordsDiffer() {
        RegisterFormDto form = validForm();
        form.setConfirmPassword("Password@124");

        Set<ConstraintViolation<RegisterFormDto>> violations = validator.validate(form);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("passwordConfirmed");
                    assertThat(violation.getMessage()).isEqualTo("Les mots de passe ne correspondent pas");
                });
    }

    @Test
    void shouldReject_whenConfirmPasswordIsMissing() {
        RegisterFormDto form = validForm();
        form.setConfirmPassword("");

        assertThat(validator.validate(form))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("confirmPassword");
    }

    private RegisterFormDto validForm() {
        RegisterFormDto form = new RegisterFormDto();
        form.setFirstname("Alice");
        form.setLastname("Martin");
        form.setUsername("alice");
        form.setEmail("alice@test.com");
        form.setPassword("Password@123");
        form.setConfirmPassword("Password@123");
        return form;
    }
}
