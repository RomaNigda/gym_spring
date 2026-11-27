package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerDaoImplTest {
    private DaoTestSupport db;
    private TrainerDaoImpl dao;
    private TrainingType yoga;

    @BeforeEach
    void setUp() {
        db = new DaoTestSupport();
        dao = new TrainerDaoImpl(db.sessionFactory());
        yoga = db.inTransaction(() -> db.persistTrainingType("YOGA"));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void savePersistsTrainerWithSpecialization() {
        // Arrange
        Trainer trainer = new Trainer("Jane", "Roe", true, yoga);
        trainer.setUsername("jane.roe");
        trainer.setPassword("secret");

        // Act
        db.inTransaction(() -> dao.save(trainer));

        // Assert
        Trainer found = db.inTransaction(() -> dao.findByUsername("jane.roe").orElseThrow());
        assertNotNull(found.getId());
        assertEquals("YOGA", found.getSpecialization().getTrainingTypeName());
    }

    @Test
    void updateMergesChanges() {
        // Arrange
        db.inTransaction(() -> db.persistTrainer("Jane", "Roe", "jane.roe", yoga));
        Trainer detached = db.inTransaction(() -> dao.findByUsername("jane.roe").orElseThrow());
        detached.setFirstName("Janet");
        detached.setActive(false);

        // Act
        db.inTransaction(() -> dao.update(detached));

        // Assert
        Trainer reloaded = db.inTransaction(() -> dao.findByUsername("jane.roe").orElseThrow());
        assertEquals("Janet", reloaded.getFirstName());
        assertEquals(false, reloaded.getActive());
    }

    @Test
    void findByUsernameLoadsAssignedTrainees() {
        // Arrange
        db.inTransaction(() -> {
            Trainer trainer = db.persistTrainer("Jane", "Roe", "jane.roe", yoga);
            Trainee trainee = db.persistTrainee("John", "Doe", "john.doe");
            trainee.getTrainers().add(trainer);
        });

        // Act
        Trainer found = db.inTransaction(() -> dao.findByUsername("jane.roe").orElseThrow());

        // Assert
        assertEquals(1, found.getTrainees().size());
        assertEquals("john.doe", found.getTrainees().iterator().next().getUsername());
    }

    @Test
    void findByUsernameReturnsEmptyWhenMissing() {
        // Arrange
        db.inTransaction(() -> db.persistTrainer("Jane", "Roe", "jane.roe", yoga));

        // Act
        boolean empty = db.inTransaction(() -> dao.findByUsername("missing").isEmpty());

        // Assert
        assertTrue(empty);
    }

    @Test
    void findByUsernamesReturnsOnlyMatchingTrainers() {
        // Arrange
        db.inTransaction(() -> {
            db.persistTrainer("Jane", "Roe", "jane.roe", yoga);
            db.persistTrainer("Bob", "Smith", "bob.smith", yoga);
            db.persistTrainer("Ann", "Lee", "ann.lee", yoga);
        });

        // Act
        List<Trainer> result = db.inTransaction(() -> dao.findByUsernames(Set.of("jane.roe", "ann.lee", "nobody")));

        // Assert
        assertEquals(Set.of("jane.roe", "ann.lee"),
                Set.copyOf(result.stream().map(Trainer::getUsername).toList()));
    }

    @Test
    void findByUsernamesReturnsEmptyListForEmptyInput() {
        // Arrange
        db.inTransaction(() -> db.persistTrainer("Jane", "Roe", "jane.roe", yoga));

        // Act
        List<Trainer> result = db.inTransaction(() -> dao.findByUsernames(List.of()));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findTrainersNotAssignedToTraineeExcludesAssignedAndSortsByUsername() {
        // Arrange
        db.inTransaction(() -> {
            Trainer assigned = db.persistTrainer("Jane", "Roe", "jane.roe", yoga);
            db.persistTrainer("Zed", "Zeta", "zed.zeta", yoga);
            db.persistTrainer("Ann", "Lee", "ann.lee", yoga);
            db.persistTrainee("John", "Doe", "john.doe").getTrainers().add(assigned);
        });

        // Act
        List<Trainer> result = db.inTransaction(() -> dao.findTrainersNotAssignedToTrainee("john.doe"));

        // Assert
        assertEquals(List.of("ann.lee", "zed.zeta"), result.stream().map(Trainer::getUsername).toList());
    }

    @Test
    void findTrainersNotAssignedToUnknownTraineeReturnsAllTrainers() {
        // Arrange
        db.inTransaction(() -> {
            db.persistTrainer("Jane", "Roe", "jane.roe", yoga);
            db.persistTrainer("Ann", "Lee", "ann.lee", yoga);
        });

        // Act
        List<Trainer> result = db.inTransaction(() -> dao.findTrainersNotAssignedToTrainee("unknown"));

        // Assert
        assertEquals(2, result.size());
    }
}
