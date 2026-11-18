package io.github.romanigda.gym_spring.exception;

import java.util.List;

public class ValidationException extends ServiceException {
    private final List<String> errors;

    public ValidationException(String message) {
        this(List.of(message));
    }

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("Validation errors must not be empty");
        }
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
