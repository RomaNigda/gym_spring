package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.UserDao;
import io.github.romanigda.gym_spring.domain.User;
import io.github.romanigda.gym_spring.exception.AuthenticationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    @Mock
    private UserDao userDao;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    void authenticateSucceedsWhenPasswordMatches() {
        // Arrange
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(user("John.Doe", "secret1234")));

        // Act
        authenticationService.authenticate("John.Doe", "secret1234");

        // Assert
        verify(userDao).findByUsername("John.Doe");
    }

    @Test
    void authenticateFailsWhenPasswordDoesNotMatch() {
        // Arrange
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(user("John.Doe", "secret1234")));

        // Act
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("John.Doe", "wrong"));

        // Assert
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void authenticateFailsWhenUserDoesNotExist() {
        // Arrange
        when(userDao.findByUsername("Unknown.User")).thenReturn(Optional.empty());

        // Act
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("Unknown.User", "secret1234"));

        // Assert
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void authenticateRejectsBlankCredentialsWithoutQueryingDatabase() {
        // Arrange
        String blankPassword = " ";

        // Act
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("John.Doe", blankPassword));
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(null, "secret1234"));

        // Assert
        verifyNoInteractions(userDao);
    }

    private static User user(String username, String password) {
        User user = new User("John", "Doe", true);
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
