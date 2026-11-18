package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerDaoImplTest {
    private TrainerDaoImpl dao;

    @BeforeEach
    void setUp() {
        Storage storage = new Storage(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new DefaultResourceLoader());
        dao = new TrainerDaoImpl(storage);
    }

    @Test
    void saveGeneratesAnIdAndStoresTheTrainer() {
        // Arrange
        Trainer trainer = trainer(101L, "trainer.one");

        // Act
        Trainer saved = dao.save(trainer);

        // Assert
        assertEquals(1L, saved.getUserId());
        assertEquals("trainer.one", dao.findById(saved.getUserId()).orElseThrow().getUsername());
        assertEquals(Set.of(TrainingType.STRENGTH), saved.getTrainingTypes());
    }

    @Test
    void updateReplacesAnExistingTrainer() {
        // Arrange
        Trainer saved = dao.save(trainer(101L, "trainer.one"));
        Trainer updated = trainer(saved.getUserId(), "trainer.updated");

        // Act
        dao.update(updated);

        // Assert
        assertEquals(updated, dao.findById(saved.getUserId()).orElseThrow());
        assertEquals(updated, dao.findByUsername("trainer.updated").orElseThrow());
        assertTrue(dao.findByUsername("trainer.one").isEmpty());
    }

    @Test
    void updateThrowsWhenTheTrainerDoesNotExist() {
        // Arrange
        Trainer missing = trainer(999L, "missing");

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> dao.update(missing));

        // Assert
        assertEquals("Trainer not found: 999", exception.getMessage());
    }

    @Test
    void deleteRemovesAnExistingTrainer() {
        // Arrange
        Trainer saved = dao.save(trainer(101L, "trainer.one"));

        // Act
        dao.delete(saved.getUserId());

        // Assert
        assertTrue(dao.findById(saved.getUserId()).isEmpty());
        assertTrue(dao.findByUsername("trainer.one").isEmpty());
    }

    @Test
    void deleteThrowsWhenTheTrainerDoesNotExist() {
        // Arrange
        Long missingId = 999L;

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> dao.delete(missingId));

        // Assert
        assertEquals("Trainer not found: 999", exception.getMessage());
    }

    @Test
    void findMethodsReturnMatchingTrainersAndFindAllIsReadOnly() {
        // Arrange
        Trainer saved = dao.save(trainer(101L, "trainer.one"));

        // Act
        var all = dao.findAll();

        // Assert
        assertEquals(saved, dao.findById(saved.getUserId()).orElseThrow());
        assertEquals(saved, dao.findByUsername("trainer.one").orElseThrow());
        assertTrue(dao.findById(999L).isEmpty());
        assertTrue(dao.findByUsername("missing").isEmpty());
        assertTrue(all.contains(saved));
        assertThrows(UnsupportedOperationException.class, () -> all.add(trainer(2L, "another")));
    }

    private Trainer trainer(Long id, String username) {
        return new Trainer("Trainer", "Test", username, "password", true,
                Set.of(TrainingType.STRENGTH), id);
    }
}
