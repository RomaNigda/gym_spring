package io.github.romanigda.gym_spring.util;

import io.github.romanigda.gym_spring.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceValidationTest {
    @Test
    void requirePositiveIdAcceptsPositiveId() {
        // Arrange
        Long id = 1L;

        // Act & Assert
        assertDoesNotThrow(() -> ServiceValidation.requirePositiveId(id, "id"));
    }

    @Test
    void requirePositiveIdRejectsNullZeroAndNegative() {
        // Arrange
        List<Long> invalid = new ArrayList<>(List.of(0L, -5L));
        invalid.add(null);

        // Act & Assert
        for (Long id : invalid) {
            assertThrows(ValidationException.class, () -> ServiceValidation.requirePositiveId(id, "id"));
        }
    }

    @Test
    void requireTextReturnsTrimmedValue() {
        // Arrange
        String value = "  hello ";

        // Act
        String result = ServiceValidation.requireText(value, "name");

        // Assert
        assertEquals("hello", result);
    }

    @Test
    void requireTextRejectsNullAndBlank() {
        // Arrange
        String blank = "   ";

        // Act & Assert
        assertThrows(ValidationException.class, () -> ServiceValidation.requireText(blank, "name"));
        assertThrows(ValidationException.class, () -> ServiceValidation.requireText(null, "name"));
    }

    @Test
    void collectMethodsAppendMessagesWithoutThrowing() {
        // Arrange
        List<String> errors = new ArrayList<>();

        // Act
        ServiceValidation.collectPositiveIdError(0L, "traineeId", errors);
        ServiceValidation.collectTextError(" ", "name", errors);
        ServiceValidation.collectPositiveIdError(3L, "ok", errors);
        ServiceValidation.collectTextError("fine", "ok", errors);

        // Assert
        assertEquals(List.of("traineeId must be a positive number", "name must not be blank"), errors);
    }

    @Test
    void throwIfErrorsDoesNothingForEmptyListAndThrowsOtherwise() {
        // Arrange
        List<String> errors = List.of("boom");

        // Act & Assert
        assertDoesNotThrow(() -> ServiceValidation.throwIfErrors(List.of()));
        assertTrue(assertThrows(ValidationException.class, () -> ServiceValidation.throwIfErrors(errors))
                .getMessage().contains("boom"));
    }
}
