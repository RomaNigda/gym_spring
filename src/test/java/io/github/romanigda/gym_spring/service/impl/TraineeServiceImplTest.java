package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.exception.DuplicateEntityException;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.service.TrainerService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeServiceImplTest {
    @Test
    void createGeneratesIdUsernameAndPassword() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            Trainee input = trainee("Service", "Unique");

            // Act
            Trainee created = service.create(input);

            // Assert
            assertEquals(6L, created.getUserId());
            assertEquals("Service.Unique", created.getUsername());
            assertTrue(created.getPassword().matches("[A-Za-z0-9]{10}"));
            assertEquals(input.getFirstName(), created.getFirstName());
            assertEquals(input.getLastName(), created.getLastName());
            assertEquals(input.getDateOfBirth(), created.getDateOfBirth());
            assertEquals(input.getAddress(), created.getAddress());
        }
    }

    @Test
    void createGeneratesUsernameUniqueAcrossTraineesAndTrainers() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService traineeService = context.getBean(TraineeService.class);
            TrainerService trainerService = context.getBean(TrainerService.class);

            // Act
            Trainee trainee = traineeService.create(trainee("Shared", "Name"));
            Trainer trainer = trainerService.create(new Trainer("Shared", "Name", true,
                    Set.of(TrainingType.STRENGTH)));

            // Assert
            assertEquals("Shared.Name", trainee.getUsername());
            assertEquals("Shared.Name1", trainer.getUsername());
        }
    }

    @Test
    void createRejectsInvalidTrainees() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            Trainee blankName = new Trainee(" ", "Name", true, LocalDate.of(1990, 1, 1), "address");
            Trainee futureBirthDate = new Trainee("Valid", "Name", true,
                    LocalDate.now().plusDays(1), "address");

            // Act
            ValidationException nullException = assertThrows(ValidationException.class,
                    () -> service.create(null));
            ValidationException blankException = assertThrows(ValidationException.class,
                    () -> service.create(blankName));
            ValidationException dateException = assertThrows(ValidationException.class,
                    () -> service.create(futureBirthDate));

            // Assert
            assertEquals("Trainee must not be null", nullException.getMessage());
            assertEquals("firstName must not be blank", blankException.getMessage());
            assertEquals("dateOfBirth must not be in the future", dateException.getMessage());
        }
    }

    @Test
    void createReturnsAllTraineeValidationErrors() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            Trainee blankNames = trainee(" ", "");

            // Act
            ValidationException exception = assertThrows(
                    ValidationException.class, () -> service.create(blankNames));

            // Assert
            assertEquals(List.of(
                    "firstName must not be blank",
                    "lastName must not be blank"), exception.getErrors());
        }
    }

    @Test
    void updateChangesTraineeAndRejectsDuplicateUsername() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            TrainerService trainerService = context.getBean(TrainerService.class);
            Trainee created = service.create(trainee("Update", "Trainee"));
            Trainer otherProfile = trainerService.create(new Trainer("Other", "Profile", true,
                    Set.of(TrainingType.STRENGTH)));
            Trainee updated = new Trainee("Update", "Changed", created.getUsername(), created.getPassword(),
                    false, created.getDateOfBirth(), "new address", created.getUserId());
            Trainee duplicateUsername = new Trainee("Update", "Changed", otherProfile.getUsername(),
                    created.getPassword(), true, created.getDateOfBirth(), "address", created.getUserId());

            // Act
            Trainee result = service.update(updated);
            DuplicateEntityException exception = assertThrows(
                    DuplicateEntityException.class, () -> service.update(duplicateUsername));

            // Assert
            assertEquals(updated.getLastName(), result.getLastName());
            assertEquals(updated.getAddress(), result.getAddress());
            assertFalse(result.isActive());
            assertEquals(result, service.selectById(created.getUserId()));
            assertTrue(exception.getMessage().contains(otherProfile.getUsername()));
        }
    }

    @Test
    void updateRejectsMissingTraineesAndInvalidIds() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            Trainee missing = new Trainee("Missing", "Trainee", "missing.user", "password",
                    true, LocalDate.of(1990, 1, 1), "address", 999L);
            Trainee invalidId = new Trainee("Invalid", "Trainee", "invalid.user", "password",
                    true, LocalDate.of(1990, 1, 1), "address", 0L);

            // Act
            EntityNotFoundException missingException = assertThrows(
                    EntityNotFoundException.class, () -> service.update(missing));
            ValidationException idException = assertThrows(
                    ValidationException.class, () -> service.update(invalidId));

            // Assert
            assertEquals("Trainee not found: 999", missingException.getMessage());
            assertEquals("trainee id must be a positive number", idException.getMessage());
        }
    }

    @Test
    void selectMethodsFindTraineesAndReportMissingProfiles() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            Trainee created = service.create(trainee("Select", "Trainee"));

            // Act
            Trainee byId = service.selectById(created.getUserId());
            Trainee byUsername = service.selectByUsername(created.getUsername());
            var all = service.selectAll();
            EntityNotFoundException missingId = assertThrows(
                    EntityNotFoundException.class, () -> service.selectById(999L));
            EntityNotFoundException missingUsername = assertThrows(
                    EntityNotFoundException.class, () -> service.selectByUsername("missing.user"));
            ValidationException invalidId = assertThrows(
                    ValidationException.class, () -> service.selectById(0L));

            // Assert
            assertEquals(created, byId);
            assertEquals(created, byUsername);
            assertTrue(all.contains(created));
            assertEquals("Trainee not found: 999", missingId.getMessage());
            assertTrue(missingUsername.getMessage().contains("missing.user"));
            assertEquals("trainee id must be a positive number", invalidId.getMessage());
        }
    }

    @Test
    void deleteRemovesTraineeAndReportsMissingProfiles() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            Trainee created = service.create(trainee("Delete", "Trainee"));

            // Act
            service.delete(created.getUserId());
            EntityNotFoundException missing = assertThrows(
                    EntityNotFoundException.class, () -> service.delete(created.getUserId()));
            ValidationException invalidId = assertThrows(
                    ValidationException.class, () -> service.delete(0L));

            // Assert
            assertFalse(service.selectAll().contains(created));
            assertEquals("Trainee not found: " + created.getUserId(), missing.getMessage());
            assertEquals("trainee id must be a positive number", invalidId.getMessage());
        }
    }

    private Trainee trainee(String firstName, String lastName) {
        return new Trainee(firstName, lastName, true, LocalDate.of(1990, 1, 1), "address");
    }
}
