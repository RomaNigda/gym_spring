package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.dao.TrainingTypeDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.domain.User;
import io.github.romanigda.gym_spring.dto.NewTraining;
import io.github.romanigda.gym_spring.dto.TraineeTrainingCriteria;
import io.github.romanigda.gym_spring.dto.TrainerTrainingCriteria;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {
    private static final TrainingType CARDIO = new TrainingType(2L, "CARDIO");
    private static final TrainingType YOGA = new TrainingType(5L, "YOGA");
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);

    @Mock
    private TrainingDao trainingDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;

    @InjectMocks
    private TrainingServiceImpl trainingService;

    @Test
    void addTrainingDefaultsTypeToTrainerSpecializationAndAssignsTrainer() {
        // Arrange
        Trainee trainee = new Trainee(user("Jane.Smith"), null, null);
        Trainer trainer = new Trainer(user("John.Doe"), CARDIO);
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainer));
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NewTraining request = new NewTraining("Jane.Smith", "John.Doe", "Morning run", null, DATE, 45);

        // Act
        Training training = trainingService.addTraining(request);

        // Assert
        assertSame(trainee, training.getTrainee());
        assertSame(trainer, training.getTrainer());
        assertSame(CARDIO, training.getTrainingType());
        assertEquals("Morning run", training.getTrainingName());
        assertEquals(DATE, training.getTrainingDate());
        assertEquals(45, training.getTrainingDuration());
        assertTrue(trainee.getTrainers().contains(trainer));
        verifyNoInteractions(trainingTypeDao);
    }

    @Test
    void addTrainingUsesExplicitTrainingType() {
        // Arrange
        when(traineeDao.findByUsername("Jane.Smith"))
                .thenReturn(Optional.of(new Trainee(user("Jane.Smith"), null, null)));
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(new Trainer(user("John.Doe"), CARDIO)));
        when(trainingTypeDao.findByName("YOGA")).thenReturn(Optional.of(YOGA));
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NewTraining request = new NewTraining("Jane.Smith", "John.Doe", "Stretch", "YOGA", DATE, 30);

        // Act
        Training training = trainingService.addTraining(request);

        // Assert
        assertSame(YOGA, training.getTrainingType());
    }

    @Test
    void addTrainingCollectsValidationErrors() {
        // Arrange
        NewTraining request = new NewTraining(" ", "John.Doe", null, null, null, 0);

        // Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> trainingService.addTraining(request));

        // Assert
        assertEquals(List.of("trainee username must not be blank", "trainingName must not be blank",
                "trainingDate must not be null", "trainingDuration must be a positive number"),
                exception.getErrors());
        verifyNoInteractions(trainingDao, traineeDao, trainerDao);
    }

    @Test
    void addTrainingThrowsWhenTrainerIsMissing() {
        // Arrange
        when(traineeDao.findByUsername("Jane.Smith"))
                .thenReturn(Optional.of(new Trainee(user("Jane.Smith"), null, null)));
        when(trainerDao.findByUsername("Ghost")).thenReturn(Optional.empty());
        NewTraining request = new NewTraining("Jane.Smith", "Ghost", "Run", null, DATE, 30);

        // Act
        assertThrows(EntityNotFoundException.class, () -> trainingService.addTraining(request));

        // Assert
        verify(trainingDao, never()).save(any());
    }

    @Test
    void selectTraineeTrainingsPassesAllCriteriaToDao() {
        // Arrange
        List<Training> expected = List.of(new Training());
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        when(trainingDao.findTraineeTrainings("Jane.Smith", from, to, "John", "CARDIO")).thenReturn(expected);

        // Act
        List<Training> result = trainingService.selectTraineeTrainings("Jane.Smith",
                new TraineeTrainingCriteria(from, to, "John", "CARDIO"));

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void selectTraineeTrainingsWithoutCriteriaDoesNotFilter() {
        // Arrange
        when(trainingDao.findTraineeTrainings("Jane.Smith", null, null, null, null)).thenReturn(List.of());

        // Act
        List<Training> result = trainingService.selectTraineeTrainings("Jane.Smith", null);

        // Assert
        assertTrue(result.isEmpty());
        verify(trainingDao).findTraineeTrainings("Jane.Smith", null, null, null, null);
    }

    @Test
    void selectTrainerTrainingsPassesAllCriteriaToDao() {
        // Arrange
        List<Training> expected = List.of(new Training());
        LocalDate from = LocalDate.of(2026, 1, 1);
        when(trainingDao.findTrainerTrainings("John.Doe", from, null, "Jane")).thenReturn(expected);

        // Act
        List<Training> result = trainingService.selectTrainerTrainings("John.Doe",
                new TrainerTrainingCriteria(from, null, "Jane"));

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void selectTrainingsRejectsInvertedDateRange() {
        // Arrange
        TrainerTrainingCriteria criteria = new TrainerTrainingCriteria(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null);

        // Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> trainingService.selectTrainerTrainings("John.Doe", criteria));

        // Assert
        assertEquals("fromDate must not be after toDate", exception.getMessage());
        verifyNoInteractions(trainingDao);
    }

    private static User user(String username) {
        User user = new User("First", "Last", true);
        user.setUsername(username);
        user.setPassword("password12");
        return user;
    }
}
