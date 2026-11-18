package io.github.romanigda.gym_spring.exception;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationExceptionTest {
    @Test
    void messageIncludesEveryErrorInOrder() {
        // Arrange
        List<String> errors = List.of("firstName must not be blank", "lastName must not be blank");

        // Act
        ValidationException exception = new ValidationException(errors);

        // Assert
        assertEquals("firstName must not be blank; lastName must not be blank", exception.getMessage());
    }

    @Test
    void errorsAreAnImmutableSnapshot() {
        // Arrange
        List<String> originalErrors = List.of("firstName must not be blank", "lastName must not be blank");
        List<String> errors = new ArrayList<>(originalErrors);

        // Act
        ValidationException exception = new ValidationException(errors);
        errors.clear();
        List<String> reportedErrors = exception.getErrors();

        // Assert
        assertEquals(originalErrors, reportedErrors);
        assertThrows(UnsupportedOperationException.class, reportedErrors::clear);
    }
}
