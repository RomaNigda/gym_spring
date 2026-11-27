package io.github.romanigda.gym_spring.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.github.romanigda.gym_spring.domain.PersistenceTestSupport.newTrainee;
import static io.github.romanigda.gym_spring.domain.PersistenceTestSupport.newTrainer;
import static io.github.romanigda.gym_spring.domain.PersistenceTestSupport.persistTrainingType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraineeMappingTest {
    private PersistenceTestSupport db;
    private Long traineeId;
    private Long trainerId;
    private Long trainingId;

    @BeforeEach
    void setUp() {
        db = new PersistenceTestSupport();
        db.inTransaction(session -> {
            TrainingType type = persistTrainingType(session);
            Trainee trainee = newTrainee("john.doe");
            Trainer trainer = newTrainer("jane.roe", type);
            session.persist(trainee);
            session.persist(trainer);
            Training training = new Training(trainee, trainer, "Morning", type, LocalDate.of(2025, 1, 1), 30);
            session.persist(training);
            trainee.getTrainings().add(training);
            traineeId = trainee.getId();
            trainerId = trainer.getId();
            trainingId = training.getId();
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void removingTraineeRemovesTheirTrainingsButKeepsTrainer() {
        // Arrange
        // (trainee, trainer and a training between them are persisted in setUp)

        // Act
        db.inTransaction(session -> session.remove(session.get(Trainee.class, traineeId)));

        // Assert
        db.inTransaction(session -> {
            assertNull(session.get(Trainee.class, traineeId));
            assertNull(session.get(Training.class, trainingId));
            assertNotNull(session.get(Trainer.class, trainerId));
        });
    }

    @Test
    void detachingTrainingFromTraineeDeletesIt() {
        // Arrange
        // (trainee, trainer and a training between them are persisted in setUp)

        // Act
        db.inTransaction(session -> session.get(Trainee.class, traineeId).getTrainings().clear());

        // Assert
        db.inTransaction(session -> {
            assertNotNull(session.get(Trainee.class, traineeId));
            assertNull(session.get(Training.class, trainingId));
        });
    }
}
