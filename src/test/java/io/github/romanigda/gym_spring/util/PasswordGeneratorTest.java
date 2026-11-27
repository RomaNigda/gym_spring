package io.github.romanigda.gym_spring.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {
    @Test
    void generatesTenAlphanumericCharacters() {
        // Arrange
        // (no setup needed)

        // Act
        String password = PasswordGenerator.generatePassword();

        // Assert
        assertEquals(10, password.length());
        assertTrue(password.matches("[A-Za-z0-9]{10}"));
    }

    @Test
    void generatesDifferentPasswordsOnSubsequentCalls() {
        // Arrange
        String first = PasswordGenerator.generatePassword();

        // Act
        String second = PasswordGenerator.generatePassword();

        // Assert
        assertNotEquals(first, second);
    }
}
