package io.github.romanigda.gym_spring;

import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.service.TrainerService;
import io.github.romanigda.gym_spring.service.TrainingService;
import io.github.romanigda.gym_spring.exception.DuplicateEntityException;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTests {

    @Test
    void profileServicesGenerateGlobalUsernamesAndCredentials() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService traineeService = context.getBean(TraineeService.class);
            TrainerService trainerService = context.getBean(TrainerService.class);

            Trainee trainee = traineeService.create(new Trainee("Alex", "Unique", true,
                    LocalDate.of(1990, 1, 1), "address"));
            Trainer trainer = trainerService.create(new Trainer("Alex", "Unique", true,
                    Set.of(TrainingType.STRENGTH)));

            assertEquals(6L, trainee.getUserId());
            assertEquals(6L, trainer.getUserId());
            assertEquals("Alex.Unique", trainee.getUsername());
            assertEquals("Alex.Unique1", trainer.getUsername());
            assertTrue(trainee.getPassword().matches("[A-Za-z0-9]{10}"));
            assertTrue(trainer.getPassword().matches("[A-Za-z0-9]{10}"));
            assertEquals(trainee, traineeService.selectByUsername("Alex.Unique"));
            assertEquals(trainer, trainerService.selectById(trainer.getUserId()));

            Trainee nextTrainee = traineeService.create(trainee(1L, "ignored", "Alex", "Unique"));
            Trainer nextTrainer = trainerService.create(trainer(999L, "ignored", "Other", "Trainer"));
            assertEquals(7L, nextTrainee.getUserId());
            assertEquals(7L, nextTrainer.getUserId());
            assertEquals("Alex.Unique2", nextTrainee.getUsername());
            assertEquals("Other.Trainer", nextTrainer.getUsername());

            Trainer duplicateUsername = new Trainer("Alex", "Unique", "Alex.Unique", "password",
                    true, Set.of(TrainingType.STRENGTH), trainer.getUserId());
            assertThrows(DuplicateEntityException.class, () -> trainerService.update(duplicateUsername));
            Trainer updatedTrainer = new Trainer("Alex", "Updated", trainer.getUsername(),
                    trainer.getPassword(), true, Set.of(TrainingType.CARDIO), trainer.getUserId());
            trainerService.update(updatedTrainer);
            assertEquals("Updated", trainerService.selectById(trainer.getUserId()).getLastName());
        }
    }

    @Test
    void generatedProfileIdsCanBeUsedToLinkATraining() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService traineeService = context.getBean(TraineeService.class);
            TrainerService trainerService = context.getBean(TrainerService.class);
            TrainingService trainingService = context.getBean(TrainingService.class);
            Trainee trainee = traineeService.create(new Trainee("New", "Trainee", true,
                    LocalDate.of(1990, 1, 1), "address"));
            Trainer trainer = trainerService.create(new Trainer("New", "Trainer", true,
                    Set.of(TrainingType.STRENGTH)));

            Training created = trainingService.create(new Training(
                    trainee.getUserId(), trainer.getUserId(), "Training", TrainingType.STRENGTH,
                    LocalDateTime.of(2026, 12, 1, 10, 0), 60L));

            assertEquals(6L, created.getId());
            assertEquals(trainee, traineeService.selectById(created.getTraineeId()));
            assertEquals(trainer, trainerService.selectById(created.getTrainerId()));
            assertEquals(created, trainingService.selectById(created.getId()));
        }
    }

    @Test
    void traineeServiceSupportsUpdateDeleteAndDomainErrors() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService service = context.getBean(TraineeService.class);
            Trainee created = service.create(trainee(null, null, "Trainee", "New"));

            Trainee updated = new Trainee("Trainee", "Updated", created.getUsername(),
                    created.getPassword(), true, LocalDate.of(1990, 1, 1), "address", created.getUserId());
            Trainee saved = service.update(updated);
            assertEquals(updated.getUsername(), service.selectById(created.getUserId()).getUsername());
            assertTrue(service.selectAll().stream()
                    .anyMatch(trainee -> trainee.getUserId().equals(saved.getUserId())));

            assertThrows(EntityNotFoundException.class, () -> service.selectById(999L));
            assertThrows(ValidationException.class,
                    () -> service.create(trainee(null, null, " ", "Invalid")));
            assertThrows(ValidationException.class,
                    () -> service.update(trainee(null, "missing.id", "Trainee", "Invalid")));

            service.delete(created.getUserId());
            assertFalse(service.selectAll().stream()
                    .anyMatch(trainee -> trainee.getUserId().equals(saved.getUserId())));
            assertThrows(EntityNotFoundException.class, () -> service.delete(created.getUserId()));
        }
    }

    @Test
    void trainingServiceCreatesAndSelectsValidatedTraining() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainingService service = context.getBean(TrainingService.class);
            Training training = new Training(101L, 1L, 1L, "New session", TrainingType.STRENGTH,
                    LocalDateTime.of(2026, 12, 1, 10, 0), 60L);

            Training created = service.create(training);
            assertEquals(6L, created.getId());
            assertEquals(training.getTraineeId(), created.getTraineeId());
            assertEquals(training.getTrainerId(), created.getTrainerId());
            assertEquals(training.getTrainingName(), created.getTrainingName());
            assertEquals(training.getTrainingType(), created.getTrainingType());
            assertEquals(training.getTrainingDateTime(), created.getTrainingDateTime());
            assertEquals(training.getTrainingDuration(), created.getTrainingDuration());
            assertEquals(created, service.selectById(created.getId()));
            assertTrue(service.selectAll().contains(created));
            assertEquals(7L, service.create(training).getId());
            assertThrows(EntityNotFoundException.class, () -> service.selectById(999L));
            assertThrows(ValidationException.class, () -> service.create(null));
        }
    }

    private Trainee trainee(Long id, String username, String firstName, String lastName) {
        return new Trainee(firstName, lastName, username, null, true,
                LocalDate.of(1990, 1, 1), "address", id);
    }

    private Trainer trainer(Long id, String username, String firstName, String lastName) {
        return new Trainer(firstName, lastName, username, null, true,
                Set.of(TrainingType.STRENGTH), id);
    }
}
