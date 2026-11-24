package io.github.romanigda.gym_spring.facade;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.User;
import io.github.romanigda.gym_spring.exception.AuthenticationException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
        Trainee input = new Trainee(new User("Jane", "Smith", true), LocalDate.of(1995, 5, 20), null);
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
    void updateTraineeUsesAuthenticatedUsername() {
        // Arrange
        Trainee input = new Trainee(new User("Jane", "Smith", true), null, "Kyiv");
        input.getUser().setUsername("Someone.Else");
        when(traineeService.update(input)).thenReturn(input);

        // Act
        Trainee updated = facade.updateTrainee("Jane.Smith", "secret1234", input);

        // Assert
        assertEquals("Jane.Smith", updated.getUser().getUsername());
    }
}
