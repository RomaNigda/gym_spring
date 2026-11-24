package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.dao.TrainingTypeDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.domain.User;
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
        User generated = user("John", "Doe", "John.Doe", true);
        when(trainingTypeDao.findByName("cardio")).thenReturn(Optional.of(CARDIO));
        when(credentialsGenerator.createUser("John", "Doe")).thenReturn(generated);
        when(trainerDao.save(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Trainer input = new Trainer(new User("John", "Doe", false), new TrainingType(null, "cardio"));

        // Act
        Trainer created = trainerService.create(input);

        // Assert
        assertSame(generated, created.getUser());
        assertSame(CARDIO, created.getSpecialization());
    }

    @Test
    void createThrowsWhenSpecializationDoesNotExist() {
        // Arrange
        when(trainingTypeDao.findById(99L)).thenReturn(Optional.empty());
        Trainer input = new Trainer(new User("John", "Doe", true), new TrainingType(99L, null));

        // Act
        assertThrows(EntityNotFoundException.class, () -> trainerService.create(input));

        // Assert
        verify(trainerDao, never()).save(any());
        verifyNoInteractions(credentialsGenerator);
    }

    @Test
    void createRejectsMissingRequiredFields() {
        // Arrange
        Trainer input = new Trainer(new User("", "Doe", true), null);

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
        Trainer existing = new Trainer(user("John", "Doe", "John.Doe", true), CARDIO);
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(existing));
        when(trainingTypeDao.findById(5L)).thenReturn(Optional.of(YOGA));
        when(trainerDao.update(existing)).thenReturn(existing);
        Trainer input = new Trainer(user("Johnny", "Doer", "John.Doe", true), new TrainingType(5L, null));

        // Act
        Trainer updated = trainerService.update(input);

        // Assert
        assertEquals("Johnny", updated.getUser().getFirstName());
        assertEquals("Doer", updated.getUser().getLastName());
        assertSame(YOGA, updated.getSpecialization());
    }

    @Test
    void updateUsesUsernameAndIgnoresInputIds() {
        // Arrange
        Trainer existing = new Trainer(user("John", "Doe", "John.Doe", true), CARDIO);
        existing.setId(1L);
        existing.getUser().setId(2L);
        Trainer input = new Trainer(user("Johnny", "Doe", "John.Doe", false), new TrainingType(5L, null));
        input.setId(99L);
        input.getUser().setId(100L);
        input.getUser().setPassword("differentPassword");
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(existing));
        when(trainingTypeDao.findById(5L)).thenReturn(Optional.of(YOGA));
        when(trainerDao.update(existing)).thenReturn(existing);

        // Act
        Trainer updated = trainerService.update(input);

        // Assert
        assertSame(existing, updated);
        assertEquals(1L, updated.getId());
        assertEquals(2L, updated.getUser().getId());
        assertEquals("password12", updated.getUser().getPassword());
        assertTrue(updated.getUser().isActive());
        verify(trainerDao).findByUsername("John.Doe");
        verify(trainerDao).update(existing);
    }

    @Test
    void toggleActiveSwitchesStateOnEveryCall() {
        // Arrange
        Trainer trainer = new Trainer(user("John", "Doe", "John.Doe", false), CARDIO);
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
        Trainer trainer = new Trainer(user("John", "Doe", "John.Doe", true), CARDIO);
        when(trainerDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainer));

        // Act
        trainerService.changePassword("John.Doe", "brandNew12");

        // Assert
        assertEquals("brandNew12", trainer.getUser().getPassword());
    }

    @Test
    void selectNotAssignedToTraineeReturnsDaoResult() {
        // Arrange
        List<Trainer> trainers = List.of(new Trainer(user("Ann", "Lee", "Ann.Lee", true), YOGA));
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

    private static User user(String firstName, String lastName, String username, boolean active) {
        User user = new User(firstName, lastName, active);
        user.setUsername(username);
        user.setPassword("password12");
        return user;
    }
}
