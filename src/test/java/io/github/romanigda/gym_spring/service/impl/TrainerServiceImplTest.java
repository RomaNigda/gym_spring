package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.dao.TrainingTypeDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.util.UserCredentialsGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {
    private static final TrainingType CARDIO = new TrainingType(2L, "CARDIO");
    private static final TrainingType YOGA = new TrainingType(5L, "YOGA");

    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private UserCredentialsGenerator credentialsGenerator;

    @InjectMocks
    private TrainerServiceImpl trainerService;

    @Test
    void createResolvesSpecializationByNameAndUsesGeneratedCredentials() {
        // Arrange
        when(trainingTypeDao.findByName("cardio")).thenReturn(Optional.of(CARDIO));
        doAnswer(invocation -> {
            Trainer profile = invocation.getArgument(0);
            profile.setUsername("John.Doe");
            profile.setPassword("password12");
            return null;
        }).when(credentialsGenerator).generateCredentials(any(Trainer.class));
        when(trainerDao.save(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Trainer input = new Trainer("John", "Doe", false, new TrainingType(null, "cardio"));

        // Act
        Trainer created = trainerService.create(input);

        // Assert
        assertEquals("John.Doe", created.getUsername());
        assertEquals("password12", created.getPassword());
        assertTrue(created.isActive());
        assertSame(CARDIO, created.getSpecialization());
        verify(credentialsGenerator).generateCredentials(created);
    }

    @Test
    void createThrowsWhenSpecializationDoesNotExist() {
        // Arrange
        when(trainingTypeDao.findById(99L)).thenReturn(Optional.empty());
        Trainer input = new Trainer("John", "Doe", true, new TrainingType(99L, null));

        // Act
        assertThrows(EntityNotFoundException.class, () -> trainerService.create(input));

        // Assert
        verify(trainerDao, never()).save(any());
        verifyNoInteractions(credentialsGenerator);
    }

    @Test
    void createRejectsMissingRequiredFields() {
        // Arrange
        Trainer input = new Trainer("", "Doe", true, null);

        // Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> trainerService.create(input));

        // Assert
        assertEquals(List.of("firstName must not be blank", "specialization must not be null"),
                exception.getErrors());
    }

    @Test
    void selectByUsernameRejectsUsersWithoutTrainerProfiles() {
        // Arrange
        when(trainerDao.findByUsername("Jane.Smith")).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> trainerService.selectByUsername("Jane.Smith"));

        // Assert
        assertEquals("Trainer not found for username: Jane.Smith", exception.getMessage());
        verify(trainerDao, never()).update(any());
    }

    @Test
    void updateChangesNamesAndSpecialization() {
        // Arrange
        Trainer existing = trainer("John", "Doe", "John.Doe", true, CARDIO);
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(existing));
        when(trainingTypeDao.findById(5L)).thenReturn(Optional.of(YOGA));
        when(trainerDao.update(existing)).thenReturn(existing);
        Trainer input = trainer("Johnny", "Doer", "John.Doe", true, new TrainingType(5L, null));

        // Act
        Trainer updated = trainerService.update(input);

        // Assert
        assertEquals("Johnny", updated.getFirstName());
        assertEquals("Doer", updated.getLastName());
        assertSame(YOGA, updated.getSpecialization());
    }

    @Test
    void updateUsesUsernameAndIgnoresInputId() {
        // Arrange
        Trainer existing = trainer("John", "Doe", "John.Doe", true, CARDIO);
        existing.setId(1L);
        Trainer input = trainer("Johnny", "Doe", "John.Doe", false, new TrainingType(5L, null));
        input.setId(99L);
        input.setPassword("differentPassword");
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(existing));
        when(trainingTypeDao.findById(5L)).thenReturn(Optional.of(YOGA));
        when(trainerDao.update(existing)).thenReturn(existing);

        // Act
        Trainer updated = trainerService.update(input);

        // Assert
        assertSame(existing, updated);
        assertEquals(1L, updated.getId());
        assertEquals("password12", updated.getPassword());
        assertTrue(updated.isActive());
        verify(trainerDao).findByUsername("John.Doe");
        verify(trainerDao).update(existing);
    }

    @Test
    void toggleActiveSwitchesStateOnEveryCall() {
        // Arrange
        Trainer trainer = trainer("John", "Doe", "John.Doe", false, CARDIO);
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainer));

        // Act
        boolean afterFirstCall = trainerService.toggleActive("John.Doe");
        boolean afterSecondCall = trainerService.toggleActive("John.Doe");

        // Assert
        assertTrue(afterFirstCall);
        assertFalse(afterSecondCall);
    }

    @Test
    void changePasswordUpdatesUserPassword() {
        // Arrange
        Trainer trainer = trainer("John", "Doe", "John.Doe", true, CARDIO);
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainer));

        // Act
        trainerService.changePassword("John.Doe", "brandNew12");

        // Assert
        assertEquals("brandNew12", trainer.getPassword());
    }

    @Test
    void selectNotAssignedToTraineeReturnsDaoResult() {
        // Arrange
        List<Trainer> trainers = List.of(trainer("Ann", "Lee", "Ann.Lee", true, YOGA));
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(new Trainee()));
        when(trainerDao.findTrainersNotAssignedToTrainee("Jane.Smith")).thenReturn(trainers);

        // Act
        List<Trainer> result = trainerService.selectNotAssignedToTrainee("Jane.Smith");

        // Assert
        assertEquals(trainers, result);
    }

    @Test
    void selectNotAssignedToTraineeThrowsWhenTraineeIsMissing() {
        // Arrange
        when(traineeDao.findByUsername("Missing.User")).thenReturn(Optional.empty());

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> trainerService.selectNotAssignedToTrainee("Missing.User"));

        // Assert
        verify(trainerDao, never()).findTrainersNotAssignedToTrainee(any());
    }

    private static Trainer trainer(String firstName, String lastName, String username, boolean active,
                                   TrainingType specialization) {
        Trainer trainer = new Trainer(firstName, lastName, active, specialization);
        trainer.setUsername(username);
        trainer.setPassword("password12");
        return trainer;
    }
}
