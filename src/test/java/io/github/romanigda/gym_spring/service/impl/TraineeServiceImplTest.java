package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.domain.User;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.util.UserCredentialsGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private UserCredentialsGenerator credentialsGenerator;

    @InjectMocks
    private TraineeServiceImpl traineeService;

    @Test
    void createUsesGeneratedCredentialsAndSavesTrainee() {
        // Arrange
        User generated = user("Jane", "Smith", "Jane.Smith1", "pass123456", true);
        when(credentialsGenerator.createUser("Jane", "Smith")).thenReturn(generated);
        when(traineeDao.save(any(Trainee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Trainee input = new Trainee(new User(" Jane ", "Smith", false), LocalDate.of(1995, 5, 20), "Kyiv");

        // Act
        Trainee created = traineeService.create(input);

        // Assert
        assertSame(generated, created.getUser());
        assertEquals("Jane.Smith1", created.getUser().getUsername());
        assertEquals(LocalDate.of(1995, 5, 20), created.getDateOfBirth());
        assertEquals("Kyiv", created.getAddress());
    }

    @Test
    void createAllowsMissingOptionalFields() {
        // Arrange
        when(credentialsGenerator.createUser("Jane", "Smith"))
                .thenReturn(user("Jane", "Smith", "Jane.Smith", "pass123456", true));
        when(traineeDao.save(any(Trainee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Trainee input = new Trainee(new User("Jane", "Smith", true), null, null);

        // Act
        Trainee created = traineeService.create(input);

        // Assert
        assertEquals(null, created.getDateOfBirth());
        assertEquals(null, created.getAddress());
    }

    @Test
    void createCollectsAllValidationErrors() {
        // Arrange
        Trainee invalid = new Trainee(new User(" ", null, true), LocalDate.now().plusDays(1), "Kyiv");

        // Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> traineeService.create(invalid));

        // Assert
        assertEquals(List.of("firstName must not be blank", "lastName must not be blank",
                "dateOfBirth must not be in the future"), exception.getErrors());
        verifyNoInteractions(traineeDao, credentialsGenerator);
    }

    @Test
    void createRejectsNullTrainee() {
        // Arrange
        Trainee trainee = null;

        // Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> traineeService.create(trainee));

        // Assert
        assertEquals("Trainee and its user data must not be null", exception.getMessage());
    }

    @Test
    void selectByUsernameThrowsWhenTraineeIsMissing() {
        // Arrange
        when(traineeDao.findByUsername("Missing.User")).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> traineeService.selectByUsername("Missing.User"));

        // Assert
        assertEquals("Trainee not found for username: Missing.User", exception.getMessage());
    }

    @Test
    void changePasswordUpdatesUserPassword() {
        // Arrange
        Trainee trainee = trainee("Jane.Smith", true);
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        // Act
        traineeService.changePassword("Jane.Smith", "newPassword1");

        // Assert
        assertEquals("newPassword1", trainee.getUser().getPassword());
    }

    @Test
    void changePasswordRejectsBlankPassword() {
        // Arrange
        String blankPassword = "  ";

        // Act
        assertThrows(ValidationException.class,
                () -> traineeService.changePassword("Jane.Smith", blankPassword));

        // Assert
        verifyNoInteractions(traineeDao);
    }

    @Test
    void updateChangesProfileFieldsButKeepsCredentials() {
        // Arrange
        Trainee existing = trainee("Jane.Smith", true);
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(existing));
        when(traineeDao.update(existing)).thenReturn(existing);
        User changes = user("Janet", "Smyth", "Jane.Smith", null, false);
        Trainee input = new Trainee(changes, LocalDate.of(1990, 1, 1), "Lviv");

        // Act
        Trainee updated = traineeService.update(input);

        // Assert
        assertEquals("Janet", updated.getUser().getFirstName());
        assertEquals("Smyth", updated.getUser().getLastName());
        assertEquals("Jane.Smith", updated.getUser().getUsername());
        assertEquals("password12", updated.getUser().getPassword());
        assertTrue(updated.getUser().isActive());
        assertEquals(LocalDate.of(1990, 1, 1), updated.getDateOfBirth());
        assertEquals("Lviv", updated.getAddress());
    }

    @Test
    void toggleActiveSwitchesStateOnEveryCall() {
        // Arrange
        Trainee trainee = trainee("Jane.Smith", true);
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        // Act
        boolean afterFirstCall = traineeService.toggleActive("Jane.Smith");
        boolean afterSecondCall = traineeService.toggleActive("Jane.Smith");

        // Assert
        assertFalse(afterFirstCall);
        assertTrue(afterSecondCall);
        assertTrue(trainee.getUser().isActive());
    }

    @Test
    void deleteByUsernameRemovesTrainee() {
        // Arrange
        Trainee trainee = trainee("Jane.Smith", true);
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        // Act
        traineeService.deleteByUsername("Jane.Smith");

        // Assert
        verify(traineeDao).delete(trainee);
    }

    @Test
    void deleteByUsernameThrowsWhenTraineeIsMissing() {
        // Arrange
        when(traineeDao.findByUsername("Missing.User")).thenReturn(Optional.empty());

        // Act
        assertThrows(EntityNotFoundException.class, () -> traineeService.deleteByUsername("Missing.User"));

        // Assert
        verify(traineeDao, never()).delete(any());
    }

    @Test
    void updateTrainersReplacesTrainersList() {
        // Arrange
        Trainee trainee = trainee("Jane.Smith", true);
        trainee.getTrainers().add(trainer("Old.Trainer"));
        Trainer first = trainer("John.Doe");
        Trainer second = trainer("Ann.Lee");
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsernames(anyCollection())).thenReturn(List.of(first, second));

        // Act
        Trainee updated = traineeService.updateTrainers("Jane.Smith", List.of("John.Doe", "Ann.Lee", "John.Doe"));

        // Assert
        assertEquals(Set.of(first, second), updated.getTrainers());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<String>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(trainerDao).findByUsernames(captor.capture());
        assertEquals(Set.of("John.Doe", "Ann.Lee"), Set.copyOf(captor.getValue()));
    }

    @Test
    void updateTrainersThrowsWhenSomeTrainersAreMissing() {
        // Arrange
        Trainee trainee = trainee("Jane.Smith", true);
        when(traineeDao.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsernames(anyCollection())).thenReturn(List.of(trainer("John.Doe")));

        // Act
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> traineeService.updateTrainers("Jane.Smith", List.of("John.Doe", "Ghost.Trainer")));

        // Assert
        assertEquals("Trainers not found: [Ghost.Trainer]", exception.getMessage());
        assertTrue(trainee.getTrainers().isEmpty());
    }

    private static Trainee trainee(String username, boolean active) {
        return new Trainee(user("Jane", "Smith", username, "password12", active), LocalDate.of(1995, 5, 20), "Kyiv");
    }

    private static Trainer trainer(String username) {
        return new Trainer(user("T", "R", username, "password12", true), new TrainingType(1L, "CARDIO"));
    }

    private static User user(String firstName, String lastName, String username, String password, boolean active) {
        User user = new User(firstName, lastName, active);
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
