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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerServiceImplTest {
    @Test
    void createGeneratesIdUsernameAndPassword() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainerService service = context.getBean(TrainerService.class);
            Trainer input = trainer("Service", "Trainer");

            // Act
            Trainer created = service.create(input);

            // Assert
            assertEquals(6L, created.getUserId());
            assertEquals("Service.Trainer", created.getUsername());
            assertTrue(created.getPassword().matches("[A-Za-z0-9]{10}"));
            assertEquals(input.getTrainingTypes(), created.getTrainingTypes());
        }
    }

    @Test
    void createGeneratesUsernameUniqueAcrossTraineesAndTrainers() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService traineeService = context.getBean(TraineeService.class);
            TrainerService trainerService = context.getBean(TrainerService.class);

            // Act
            Trainee trainee = traineeService.create(new Trainee("Shared", "Name", true,
                    LocalDate.of(1990, 1, 1), "address"));
            Trainer trainer = trainerService.create(trainer("Shared", "Name"));

            // Assert
            assertEquals("Shared.Name", trainee.getUsername());
            assertEquals("Shared.Name1", trainer.getUsername());
        }
    }

    @Test
    void createRejectsNullTrainerAndEmptyTrainingTypes() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainerService service = context.getBean(TrainerService.class);
            Trainer noTrainingTypes = new Trainer("Valid", "Trainer", true, Set.of());

            // Act
            ValidationException nullException = assertThrows(
                    ValidationException.class, () -> service.create(null));
            ValidationException typesException = assertThrows(
                    ValidationException.class, () -> service.create(noTrainingTypes));

            // Assert
            assertEquals("Trainer must not be null", nullException.getMessage());
            assertEquals("trainingTypes must not be empty", typesException.getMessage());
        }
    }

    @Test
    void updateChangesTrainerAndRejectsDuplicateUsername() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainerService service = context.getBean(TrainerService.class);
            TraineeService traineeService = context.getBean(TraineeService.class);
            Trainer created = service.create(trainer("Update", "Trainer"));
            Trainee otherProfile = traineeService.create(new Trainee("Other", "Profile", true,
                    LocalDate.of(1990, 1, 1), "address"));
            Trainer updated = new Trainer("Update", "Changed", created.getUsername(), created.getPassword(),
                    false, Set.of(TrainingType.CARDIO), created.getUserId());
            Trainer duplicateUsername = new Trainer("Update", "Changed", otherProfile.getUsername(),
                    created.getPassword(), true, Set.of(TrainingType.STRENGTH), created.getUserId());

            // Act
            Trainer result = service.update(updated);
            DuplicateEntityException exception = assertThrows(
                    DuplicateEntityException.class, () -> service.update(duplicateUsername));

            // Assert
            assertEquals(updated.getLastName(), result.getLastName());
            assertEquals(Set.of(TrainingType.CARDIO), result.getTrainingTypes());
            assertEquals(result, service.selectById(created.getUserId()));
            assertTrue(exception.getMessage().contains(otherProfile.getUsername()));
        }
    }

    @Test
    void updateRejectsMissingTrainersAndInvalidIds() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainerService service = context.getBean(TrainerService.class);
            Trainer missing = new Trainer("Missing", "Trainer", "missing.user", "password",
                    true, Set.of(TrainingType.STRENGTH), 999L);
            Trainer invalidId = new Trainer("Invalid", "Trainer", "invalid.user", "password",
                    true, Set.of(TrainingType.STRENGTH), 0L);

            // Act
            EntityNotFoundException missingException = assertThrows(
                    EntityNotFoundException.class, () -> service.update(missing));
            ValidationException idException = assertThrows(
                    ValidationException.class, () -> service.update(invalidId));

            // Assert
            assertEquals("Trainer not found: 999", missingException.getMessage());
            assertEquals("trainer id must be a positive number", idException.getMessage());
        }
    }

    @Test
    void selectMethodsFindTrainersAndReportMissingProfiles() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainerService service = context.getBean(TrainerService.class);
            Trainer created = service.create(trainer("Select", "Trainer"));

            // Act
            Trainer byId = service.selectById(created.getUserId());
            Trainer byUsername = service.selectByUsername(created.getUsername());
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
            assertEquals("Trainer not found: 999", missingId.getMessage());
            assertTrue(missingUsername.getMessage().contains("missing.user"));
            assertEquals("trainer id must be a positive number", invalidId.getMessage());
        }
    }

    private Trainer trainer(String firstName, String lastName) {
        return new Trainer(firstName, lastName, true, Set.of(TrainingType.STRENGTH));
    }
}
