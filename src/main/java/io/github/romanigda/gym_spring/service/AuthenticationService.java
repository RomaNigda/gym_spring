package io.github.romanigda.gym_spring.service;

public interface AuthenticationService {
    /**
     * Checks that a user with the given username exists and the password matches.
     *
     * @throws io.github.romanigda.gym_spring.exception.AuthenticationException if credentials are invalid
     */
    void authenticate(String username, String password);
}
