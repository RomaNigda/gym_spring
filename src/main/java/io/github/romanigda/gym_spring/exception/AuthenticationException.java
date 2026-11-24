package io.github.romanigda.gym_spring.exception;

public class AuthenticationException extends ServiceException {
    public AuthenticationException(String message) {
        super(message);
    }
}
