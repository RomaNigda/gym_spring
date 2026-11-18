package io.github.romanigda.gym_spring.util;

import io.github.romanigda.gym_spring.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

public final class ServiceValidation {
    private ServiceValidation() {
    }

    public static void requirePositiveId(Long id, String fieldName) {
        List<String> errors = new ArrayList<>();
        collectPositiveIdError(id, fieldName, errors);
        throwIfErrors(errors);
    }

    public static String requireText(String value, String fieldName) {
        List<String> errors = new ArrayList<>();
        collectTextError(value, fieldName, errors);
        throwIfErrors(errors);
        return value.trim();
    }

    public static void collectPositiveIdError(Long id, String fieldName, List<String> errors) {
        if (id == null || id <= 0) {
            errors.add(fieldName + " must be a positive number");
        }
    }

    public static void collectTextError(String value, String fieldName, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(fieldName + " must not be blank");
        }
    }

    public static void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
