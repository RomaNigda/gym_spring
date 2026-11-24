package io.github.romanigda.gym_spring.util;

import io.github.romanigda.gym_spring.dao.UserDao;
import io.github.romanigda.gym_spring.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialsGeneratorTest {
    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserCredentialsGenerator generator;

    @Test
    void createUserUsesBaseUsernameWhenItIsFree() {
        // Arrange
        when(userDao.findUsernamesStartingWith("John.Smith")).thenReturn(List.of());

        // Act
        User user = generator.createUser("John", "Smith");

        // Assert
        assertEquals("John.Smith", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertTrue(user.isActive());
        assertTrue(user.getPassword().matches("[A-Za-z0-9]{10}"));
    }

    @Test
    void createUserAddsSerialNumberWhenUsernameAlreadyExists() {
        // Arrange
        when(userDao.findUsernamesStartingWith("John.Smith"))
                .thenReturn(List.of("John.Smith", "John.Smith1", "John.Smithson"));

        // Act
        User user = generator.createUser("John", "Smith");

        // Assert
        assertEquals("John.Smith2", user.getUsername());
    }

    @Test
    void usernameGeneratorSkipsTakenSerialNumbers() {
        // Arrange
        Set<String> existing = Set.of("Anna.Lee", "Anna.Lee1", "Anna.Lee3");

        // Act
        String username = UsernameGenerator.generateUsername("Anna", "Lee", existing);

        // Assert
        assertEquals("Anna.Lee2", username);
    }
}
