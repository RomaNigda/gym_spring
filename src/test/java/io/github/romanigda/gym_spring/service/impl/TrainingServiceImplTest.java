package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingServiceImplTest {
    @Test
    void createGeneratesIdAndPersistsAllTrainingDetails() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainingService service = context.getBean(TrainingService.class);
            Training input = training("New session");

            // Act
            Training created = service.create(input);

            // Assert
            assertEquals(6L, created.getId());
            assertEquals(input.getTraineeId(), created.getTraineeId());
            assertEquals(input.getTrainerId(), created.getTrainerId());
            assertEquals(input.getTrainingName(), created.getTrainingName());
            assertEquals(input.getTrainingType(), created.getTrainingType());
            assertEquals(input.getTrainingDateTime(), created.getTrainingDateTime());
            assertEquals(input.getTrainingDuration(), created.getTrainingDuration());
            assertEquals(created, service.selectById(created.getId()));
            assertTrue(service.selectAll().contains(created));
        }
    }

    @Test
    void createRejectsInvalidTrainings() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainingService service = context.getBean(TrainingService.class);
            Training invalidDuration = new Training(1L, 1L, "Invalid", TrainingType.STRENGTH,
                    LocalDateTime.of(2026, 12, 1, 10, 0), 0L);

            // Act
            ValidationException nullException = assertThrows(
                    ValidationException.class, () -> service.create(null));
            ValidationException durationException = assertThrows(
                    ValidationException.class, () -> service.create(invalidDuration));

            // Assert
            assertEquals("Training must not be null", nullException.getMessage());
            assertEquals("trainingDuration must be a positive number", durationException.getMessage());
        }
    }

    @Test
    void selectByIdRejectsMissingAndInvalidIds() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainingService service = context.getBean(TrainingService.class);

            // Act
            EntityNotFoundException missingException = assertThrows(
                    EntityNotFoundException.class, () -> service.selectById(999L));
            ValidationException invalidException = assertThrows(
                    ValidationException.class, () -> service.selectById(0L));

            // Assert
            assertEquals("Training not found: 999", missingException.getMessage());
            assertEquals("training id must be a positive number", invalidException.getMessage());
        }
    }

    private Training training(String name) {
        return new Training(101L, 1L, 1L, name, TrainingType.STRENGTH,
                LocalDateTime.of(2026, 12, 1, 10, 0), 60L);
    }
}
