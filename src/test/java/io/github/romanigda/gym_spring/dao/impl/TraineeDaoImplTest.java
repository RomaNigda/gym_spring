package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeDaoImplTest {
    private DaoTestSupport db;
    private TraineeDaoImpl dao;

    @BeforeEach
    void setUp() {
        db = new DaoTestSupport();
        dao = new TraineeDaoImpl(db.sessionFactory());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void saveTraineeWithTrainersPersistsAssignments() {
        // Arrange
        Trainee trainee = DaoTestSupport.newTrainee("John", "Doe", "john.doe");
        db.inTransaction(() -> {
            TrainingType type = db.persistTrainingType("YOGA");
            trainee.getTrainers().add(db.persistTrainer("Jane", "Roe", "jane.roe", type));
            trainee.getTrainers().add(db.persistTrainer("Bob", "Smith", "bob.smith", type));
        });

        // Act
        db.inTransaction(() -> dao.save(trainee));

        // Assert
        Trainee found = db.inTransaction(() -> dao.findByUsername("john.doe").orElseThrow());
        assertNotNull(found.getId());
        Set<String> usernames = found.getTrainers().stream().map(Trainer::getUsername).collect(Collectors.toSet());
        assertEquals(Set.of("jane.roe", "bob.smith"), usernames);
    }

    @Test
    void deleteTraineeWithTrainersRemovesTraineeAndKeepsTrainers() {
        // Arrange
        Trainee trainee = DaoTestSupport.newTrainee("John", "Doe", "john.doe");
        db.inTransaction(() -> {
            TrainingType type = db.persistTrainingType("YOGA");
            trainee.getTrainers().add(db.persistTrainer("Jane", "Roe", "jane.roe", type));
            trainee.getTrainers().add(db.persistTrainer("Bob", "Smith", "bob.smith", type));
            dao.save(trainee);
        });
        Trainee toDelete = db.inTransaction(() -> dao.findByUsername("john.doe").orElseThrow());

        // Act
        db.inTransaction(() -> dao.delete(db.session().merge(toDelete)));

        // Assert
        assertTrue(db.inTransaction(() -> dao.findByUsername("john.doe")).isEmpty());
        db.inTransaction(() -> {
            assertEquals(2, db.session().createQuery("from Trainer", Trainer.class).getResultList().size());
            assertNull(db.session().get(Trainee.class, toDelete.getId()));
            Number links = (Number) db.session().createNativeQuery("select count(*) from trainee_trainer")
                    .getSingleResult();
            assertEquals(0, links.intValue());
        });
    }

    @Test
    void deleteTraineeAlsoRemovesTheirTrainings() {
        // Arrange
        Long[] ids = new Long[2];
        db.inTransaction(() -> {
            TrainingType type = db.persistTrainingType("YOGA");
            Trainee trainee = db.persistTrainee("John", "Doe", "john.doe");
            Trainer trainer = db.persistTrainer("Jane", "Roe", "jane.roe", type);
            Training training = db.persistTraining(trainee, trainer, "Morning", type, LocalDate.of(2025, 1, 1));
            trainee.getTrainings().add(training);
            ids[0] = trainee.getId();
            ids[1] = training.getId();
        });

        // Act
        db.inTransaction(() -> dao.delete(db.session().get(Trainee.class, ids[0])));

        // Assert
        db.inTransaction(() -> {
            assertNull(db.session().get(Training.class, ids[1]));
            assertEquals(1, db.session().createQuery("from Trainer", Trainer.class).getResultList().size());
        });
    }

    @Test
    void updateMergesChangesAndTrainerAssignments() {
        // Arrange
        db.inTransaction(() -> {
            TrainingType type = db.persistTrainingType("YOGA");
            db.persistTrainee("John", "Doe", "john.doe");
            db.persistTrainer("Jane", "Roe", "jane.roe", type);
        });
        Trainee detached = db.inTransaction(() -> dao.findByUsername("john.doe").orElseThrow());
        detached.setAddress("New street 5");
        Trainer trainer = db.inTransaction(() ->
                db.session().createQuery("from Trainer", Trainer.class).getSingleResult());
        detached.getTrainers().add(trainer);

        // Act
        db.inTransaction(() -> dao.update(detached));

        // Assert
        Trainee reloaded = db.inTransaction(() -> dao.findByUsername("john.doe").orElseThrow());
        assertEquals("New street 5", reloaded.getAddress());
        assertEquals(1, reloaded.getTrainers().size());
    }

    @Test
    void findByUsernameReturnsEmptyWhenTraineeDoesNotExist() {
        // Arrange
        db.inTransaction(() -> db.persistTrainee("John", "Doe", "john.doe"));

        // Act
        Optional<Trainee> result = db.inTransaction(() -> dao.findByUsername("missing"));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUsernameReturnsTraineeWithoutTrainers() {
        // Arrange
        db.inTransaction(() -> db.persistTrainee("John", "Doe", "john.doe"));

        // Act
        Trainee result = db.inTransaction(() -> dao.findByUsername("john.doe").orElseThrow());

        // Assert
        assertEquals("John", result.getFirstName());
        assertTrue(result.getTrainers().isEmpty());
    }
}
