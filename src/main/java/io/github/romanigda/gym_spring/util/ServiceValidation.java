package io.github.romanigda.gym_spring.util;

import io.github.romanigda.gym_spring.exception.ValidationException;

public final class ServiceValidation {
    private ServiceValidation() {
    }

    public static void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new ValidationException(fieldName + " must be a positive number");
        }
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
