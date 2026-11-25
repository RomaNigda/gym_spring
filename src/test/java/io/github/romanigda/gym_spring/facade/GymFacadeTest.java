package io.github.romanigda.gym_spring.facade;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.exception.AuthenticationException;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.AuthenticationService;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.service.TrainerService;
import io.github.romanigda.gym_spring.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainerService trainerService;
    @Mock
    private TrainingService trainingService;

    @InjectMocks
    private GymFacade facade;

    @Test
    void createTraineeDoesNotRequireAuthentication() {
        // Arrange
        Trainee input = new Trainee("Jane", "Smith", true, LocalDate.of(1995, 5, 20), null);
        when(traineeService.create(input)).thenReturn(input);

        // Act
        Trainee created = facade.createTrainee(input);

        // Assert
        assertSame(input, created);
        verifyNoInteractions(authenticationService);
    }

    @Test
    void operationIsNotExecutedWhenAuthenticationFails() {
        // Arrange
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(authenticationService).authenticate("Jane.Smith", "wrong");

        // Act
        assertThrows(AuthenticationException.class, () -> facade.deleteTrainee("Jane.Smith", "wrong"));

        // Assert
        verifyNoInteractions(traineeService, trainerService, trainingService);
    }

    @Test
    void changePasswordAuthenticatesWithOldPasswordBeforeChange() {
        // Arrange
        InOrder order = inOrder(authenticationService, traineeService);

        // Act
        facade.changeTraineePassword("Jane.Smith", "oldPass123", "newPass123");

        // Assert
        order.verify(authenticationService).authenticate("Jane.Smith", "oldPass123");
        order.verify(traineeService).changePassword("Jane.Smith", "newPass123");
    }

    @Test
    void toggleTrainerActiveAuthenticatesAndReturnsNewState() {
        // Arrange
        when(trainerService.toggleActive("John.Doe")).thenReturn(false);
        InOrder order = inOrder(authenticationService, trainerService);

        // Act
        boolean active = facade.toggleTrainerActive("John.Doe", "secret1234");

        // Assert
        assertEquals(false, active);
        order.verify(authenticationService).authenticate("John.Doe", "secret1234");
        order.verify(trainerService).toggleActive("John.Doe");
    }

    @Test
    void updateTraineeAuthenticatesTraineeAndUsesAuthenticatedUsername() {
        // Arrange
        Trainee authenticatedTrainee = new Trainee("Jane", "Smith", true, null, "Kyiv");
        Trainee input = new Trainee("Janet", "Smith", true, null, "Lviv");
        input.setId(99L);
        input.setUsername("Other.Trainee");
        when(traineeService.selectByUsername("Jane.Smith")).thenReturn(authenticatedTrainee);
        when(traineeService.update(input)).thenReturn(input);
        InOrder order = inOrder(authenticationService, traineeService);

        // Act
        Trainee updated = facade.updateTrainee("Jane.Smith", "secret1234", input);

        // Assert
        assertSame(input, updated);
        assertEquals("Jane.Smith", updated.getUsername());
        order.verify(authenticationService).authenticate("Jane.Smith", "secret1234");
        order.verify(traineeService).selectByUsername("Jane.Smith");
        order.verify(traineeService).update(input);
    }

    @Test
    void updateTraineeRejectsAuthenticatedNonTraineeBeforeChangingInput() {
        // Arrange
        Trainee input = new Trainee("Jane", "Smith", true, null, "Kyiv");
        input.setId(99L);
        input.setUsername("Other.Trainee");
        EntityNotFoundException failure = new EntityNotFoundException("Trainee not found");
        when(traineeService.selectByUsername("John.Doe")).thenThrow(failure);
        InOrder order = inOrder(authenticationService, traineeService);

        // Act
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> facade.updateTrainee("John.Doe", "secret1234", input));

        // Assert
        assertSame(failure, exception);
        assertEquals("Other.Trainee", input.getUsername());
        order.verify(authenticationService).authenticate("John.Doe", "secret1234");
        order.verify(traineeService).selectByUsername("John.Doe");
        verify(traineeService, never()).update(any());
    }

    @Test
    void updateTraineeDoesNotCheckTraineeWhenAuthenticationFails() {
        // Arrange
        Trainee input = new Trainee("Jane", "Smith", true, null, "Kyiv");
        input.setUsername("Other.Trainee");
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(authenticationService).authenticate("Jane.Smith", "wrong");

        // Act
        assertThrows(AuthenticationException.class, () -> facade.updateTrainee("Jane.Smith", "wrong", input));

        // Assert
        assertEquals("Other.Trainee", input.getUsername());
        verifyNoInteractions(traineeService);
    }

    @Test
    void updateTraineeRejectsMissingTrainee() {
        // Arrange
        Trainee authenticatedTrainee = new Trainee("Jane", "Smith", true, null, "Kyiv");
        when(traineeService.selectByUsername("Jane.Smith")).thenReturn(authenticatedTrainee);

        // Act
        ValidationException missingTrainee = assertThrows(ValidationException.class,
                () -> facade.updateTrainee("Jane.Smith", "secret1234", null));

        // Assert
        assertEquals("Trainee and its user data must not be null", missingTrainee.getMessage());
        verify(traineeService).selectByUsername("Jane.Smith");
        verify(traineeService, never()).update(any());
    }

    @Test
    void updateTrainerAuthenticatesTrainerAndUsesAuthenticatedUsername() {
        // Arrange
        TrainingType specialization = new TrainingType(1L, "CARDIO");
        Trainer authenticatedTrainer = new Trainer("John", "Doe", true, specialization);
        Trainer input = new Trainer("Johnny", "Doe", true, specialization);
        input.setId(99L);
        input.setUsername("Other.Trainer");
        when(trainerService.selectByUsername("John.Doe")).thenReturn(authenticatedTrainer);
        when(trainerService.update(input)).thenReturn(input);
        InOrder order = inOrder(authenticationService, trainerService);

        // Act
        Trainer updated = facade.updateTrainer("John.Doe", "secret1234", input);

        // Assert
        assertSame(input, updated);
        assertEquals("John.Doe", updated.getUsername());
        order.verify(authenticationService).authenticate("John.Doe", "secret1234");
        order.verify(trainerService).selectByUsername("John.Doe");
        order.verify(trainerService).update(input);
    }

    @Test
    void updateTrainerRejectsAuthenticatedNonTrainerBeforeChangingInput() {
        // Arrange
        Trainer input = new Trainer("John", "Doe", true, new TrainingType(1L, "CARDIO"));
        input.setId(99L);
        input.setUsername("Other.Trainer");
        EntityNotFoundException failure = new EntityNotFoundException("Trainer not found");
        when(trainerService.selectByUsername("Jane.Smith")).thenThrow(failure);
        InOrder order = inOrder(authenticationService, trainerService);

        // Act
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> facade.updateTrainer("Jane.Smith", "secret1234", input));

        // Assert
        assertSame(failure, exception);
        assertEquals("Other.Trainer", input.getUsername());
        order.verify(authenticationService).authenticate("Jane.Smith", "secret1234");
        order.verify(trainerService).selectByUsername("Jane.Smith");
        verify(trainerService, never()).update(any());
    }

    @Test
    void updateTrainerDoesNotCheckTrainerWhenAuthenticationFails() {
        // Arrange
        Trainer input = new Trainer("John", "Doe", true, new TrainingType(1L, "CARDIO"));
        input.setUsername("Other.Trainer");
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(authenticationService).authenticate("John.Doe", "wrong");

        // Act
        assertThrows(AuthenticationException.class, () -> facade.updateTrainer("John.Doe", "wrong", input));

        // Assert
        assertEquals("Other.Trainer", input.getUsername());
        verifyNoInteractions(trainerService);
    }

    @Test
    void updateTrainerRejectsMissingTrainer() {
        // Arrange
        Trainer authenticatedTrainer = new Trainer("John", "Doe", true, null);
        when(trainerService.selectByUsername("John.Doe")).thenReturn(authenticatedTrainer);

        // Act
        ValidationException missingTrainer = assertThrows(ValidationException.class,
                () -> facade.updateTrainer("John.Doe", "secret1234", null));

        // Assert
        assertEquals("Trainer and its user data must not be null", missingTrainer.getMessage());
        verify(trainerService).selectByUsername("John.Doe");
        verify(trainerService, never()).update(any());
    }
}
