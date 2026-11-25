package io.github.romanigda.gym_spring.domain;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserInheritanceMappingTest {
    @Test
    void profilesInheritUserFieldsThroughJoinedTables() {
        // Arrange
        Inheritance inheritance = User.class.getAnnotation(Inheritance.class);
        PrimaryKeyJoinColumn traineeKey = Trainee.class.getAnnotation(PrimaryKeyJoinColumn.class);
        PrimaryKeyJoinColumn trainerKey = Trainer.class.getAnnotation(PrimaryKeyJoinColumn.class);

        // Act
        Class<?> traineeParent = Trainee.class.getSuperclass();
        Class<?> trainerParent = Trainer.class.getSuperclass();

        // Assert
        assertNotNull(inheritance);
        assertEquals(InheritanceType.JOINED, inheritance.strategy());
        assertEquals(User.class, traineeParent);
        assertEquals(User.class, trainerParent);
        assertNotNull(traineeKey);
        assertNotNull(trainerKey);
        assertEquals("id", traineeKey.name());
        assertEquals("id", trainerKey.name());
        assertThrows(NoSuchFieldException.class, () -> Trainee.class.getDeclaredField("user"));
        assertThrows(NoSuchFieldException.class, () -> Trainer.class.getDeclaredField("user"));
        assertThrows(NoSuchFieldException.class, () -> Trainee.class.getDeclaredField("id"));
        assertThrows(NoSuchFieldException.class, () -> Trainer.class.getDeclaredField("id"));
    }
}
