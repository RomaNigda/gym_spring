package com.example.gym_spring.exception;

public class ValidationException extends ServiceException {
    public ValidationException(String message) {
        super(message);
    }
}
