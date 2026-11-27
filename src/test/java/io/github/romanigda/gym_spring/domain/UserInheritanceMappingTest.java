package io.github.romanigda.gym_spring.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static io.github.romanigda.gym_spring.domain.PersistenceTestSupport.newTrainee;
import static io.github.romanigda.gym_spring.domain.PersistenceTestSupport.newTrainer;
import static io.github.romanigda.gym_spring.domain.PersistenceTestSupport.persistTrainingType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserInheritanceMappingTest {
    private PersistenceTestSupport db;

    @BeforeEach
    void setUp() {
        db = new PersistenceTestSupport();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void savedTraineeIsLoadedWithUserAndProfileFields() {
        // Arrange
        Long id = db.fromTransaction(session -> {
            Trainee trainee = newTrainee("john.doe");
            session.persist(trainee);
            return trainee.getId();
        });

        // Act
        Trainee loaded = db.fromTransaction(session -> session.get(Trainee.class, id));

        // Assert
        assertEquals("john.doe", loaded.getUsername());
        assertEquals("John", loaded.getFirstName());
        assertEquals("Street 1", loaded.getAddress());
    }

    @Test
    void savedTrainerIsLoadedWithUserAndProfileFields() {
        // Arrange
        Long id = db.fromTransaction(session -> {
            Trainer trainer = newTrainer("jane.roe", persistTrainingType(session));
            session.persist(trainer);
            return trainer.getId();
        });

        // Act
        Trainer loaded = db.fromTransaction(session -> session.get(Trainer.class, id));

        // Assert
        assertEquals("jane.roe", loaded.getUsername());
        assertEquals("Jane", loaded.getFirstName());
        assertEquals("YOGA", loaded.getSpecialization().getTrainingTypeName());
    }

    @Test
    void loadingUserByIdReturnsTheMatchingProfileType() {
        // Arrange
        Long[] ids = db.fromTransaction(session -> {
            Trainee trainee = newTrainee("john.doe");
            Trainer trainer = newTrainer("jane.roe", persistTrainingType(session));
            session.persist(trainee);
            session.persist(trainer);
            return new Long[]{trainee.getId(), trainer.getId()};
        });

        // Act
        User traineeUser = db.fromTransaction(session -> session.get(User.class, ids[0]));
        User trainerUser = db.fromTransaction(session -> session.get(User.class, ids[1]));

        // Assert
        assertInstanceOf(Trainee.class, traineeUser);
        assertInstanceOf(Trainer.class, trainerUser);
    }

    @Test
    void traineeAndTrainerCannotShareUsername() {
        // Arrange
        db.inTransaction(session -> session.persist(newTrainee("shared.name")));

        // Act
        Executable saveTrainerWithSameUsername = () -> db.inTransaction(session ->
                session.persist(newTrainer("shared.name", persistTrainingType(session))));

        // Assert
        assertThrows(PersistenceException.class, saveTrainerWithSameUsername);
    }
}
