package io.github.romanigda.gym_spring.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DomainEqualsHashCodeTest {

    @Test
    void usersWithSameUsernameAreEqual() {
        // Arrange
        Trainee first = trainee("Jane.Smith");
        Trainee second = trainee("Jane.Smith");

        // Act
        boolean equal = first.equals(second);

        // Assert
        assertEquals(true, equal);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void usersWithDifferentUsernameAreNotEqual() {
        // Arrange
        Trainee first = trainee("Jane.Smith");
        Trainee second = trainee("Jane.Smith1");

        // Act
        boolean equal = first.equals(second);

        // Assert
        assertEquals(false, equal);
    }

    @Test
    void usersWithoutUsernameAreNotEqualToEachOther() {
        // Arrange
        Trainee first = new Trainee();
        Trainee second = new Trainee();

        // Act
        boolean equal = first.equals(second);

        // Assert
        assertEquals(false, equal);
        assertEquals(true, first.equals(first));
    }

    @Test
    void trainingTypesAreEqualByName() {
        // Arrange
        TrainingType first = new TrainingType(1L, "Yoga");
        TrainingType second = new TrainingType(2L, "Yoga");

        // Act
        boolean equal = first.equals(second);

        // Assert
        assertEquals(true, equal);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new TrainingType(1L, "Cardio"));
    }

    @Test
    void trainingsAreEqualByBusinessFields() {
        // Arrange
        TrainingType type = new TrainingType(1L, "Yoga");
        Trainer trainer = new Trainer("John", "Doe", true, type);
        trainer.setUsername("John.Doe");
        Trainee trainee = trainee("Jane.Smith");
        LocalDate date = LocalDate.of(2025, 1, 1);
        Training first = new Training(trainee, trainer, "Morning", type, date, 60);
        Training second = new Training(trainee, trainer, "Morning", type, date, 60);
        Training other = new Training(trainee, trainer, "Morning", type, date, 90);

        // Act
        boolean equal = first.equals(second);

        // Assert
        assertEquals(true, equal);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

    private static Trainee trainee(String username) {
        Trainee trainee = new Trainee("Jane", "Smith", true, LocalDate.of(1990, 1, 1), "Street");
        trainee.setUsername(username);
        return trainee;
    }
}
