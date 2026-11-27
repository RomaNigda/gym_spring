package io.github.romanigda.gym_spring.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsernameGeneratorTest {
    @Test
    void returnsBaseUsernameWhenFree() {
        // Arrange
        Set<String> existing = Set.of("other.user");

        // Act
        String result = UsernameGenerator.generateUsername("John", "Doe", existing);

        // Assert
        assertEquals("John.Doe", result);
    }

    @Test
    void appendsFirstSuffixWhenBaseIsTaken() {
        // Arrange
        Set<String> existing = Set.of("John.Doe");

        // Act
        String result = UsernameGenerator.generateUsername("John", "Doe", existing);

        // Assert
        assertEquals("John.Doe1", result);
    }

    @Test
    void skipsTakenSuffixes() {
        // Arrange
        Set<String> existing = Set.of("John.Doe", "John.Doe1", "John.Doe2");

        // Act
        String result = UsernameGenerator.generateUsername("John", "Doe", existing);

        // Assert
        assertEquals("John.Doe3", result);
    }
}
