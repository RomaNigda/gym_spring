package io.github.romanigda.gym_spring.exception;

public class ValidationException extends ServiceException {
    public ValidationException(String message) {
        super(message);
    }
}
