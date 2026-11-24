package io.github.romanigda.gym_spring.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeMappingTest {
    @Test
    void traineeTrainingsAreRemovedTogetherWithTrainee() throws NoSuchFieldException {
        // Arrange
        OneToMany mapping = Trainee.class.getDeclaredField("trainings").getAnnotation(OneToMany.class);

        // Act
        boolean cascadesRemove = Arrays.asList(mapping.cascade()).contains(CascadeType.REMOVE);

        // Assert
        assertTrue(cascadesRemove);
        assertTrue(mapping.orphanRemoval());
    }
}
