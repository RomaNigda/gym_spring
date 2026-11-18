package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDaoImplTest {
    private TrainingDaoImpl dao;

    @BeforeEach
    void setUp() {
        Storage storage = new Storage(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new DefaultResourceLoader());
        dao = new TrainingDaoImpl(storage);
    }

    @Test
    void saveGeneratesAnIdAndStoresTheTraining() {
        // Arrange
        Training training = training(101L);

        // Act
        Training saved = dao.save(training);

        // Assert
        assertEquals(1L, saved.getId());
        assertEquals(training.getTraineeId(), dao.findById(saved.getId()).orElseThrow().getTraineeId());
        assertEquals(training.getTrainingName(), dao.findById(saved.getId()).orElseThrow().getTrainingName());
        assertEquals(training.getTrainingType(), dao.findById(saved.getId()).orElseThrow().getTrainingType());
        assertEquals(training.getTrainingDateTime(), dao.findById(saved.getId()).orElseThrow().getTrainingDateTime());
        assertEquals(training.getTrainingDuration(), dao.findById(saved.getId()).orElseThrow().getTrainingDuration());
    }

    @Test
    void updateReplacesAnExistingTraining() {
        // Arrange
        Training saved = dao.save(training(101L));
        Training updated = new Training(saved.getId(), 2L, 3L, "Updated training",
                TrainingType.CARDIO, LocalDateTime.of(2025, 2, 1, 11, 0), 45L);

        // Act
        dao.update(updated);

        // Assert
        assertEquals(updated, dao.findById(saved.getId()).orElseThrow());
    }

    @Test
    void updateThrowsWhenTheTrainingDoesNotExist() {
        // Arrange
        Training missing = training(999L);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> dao.update(missing));

        // Assert
        assertEquals("Training not found: 999", exception.getMessage());
    }

    @Test
    void deleteRemovesAnExistingTraining() {
        // Arrange
        Training saved = dao.save(training(101L));

        // Act
        dao.delete(saved.getId());

        // Assert
        assertTrue(dao.findById(saved.getId()).isEmpty());
    }

    @Test
    void deleteThrowsWhenTheTrainingDoesNotExist() {
        // Arrange
        Long missingId = 999L;

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> dao.delete(missingId));

        // Assert
        assertEquals("Training not found: 999", exception.getMessage());
    }

    @Test
    void findByIdAndFindAllReturnStoredTrainings() {
        // Arrange
        Training saved = dao.save(training(101L));

        // Act
        var all = dao.findAll();

        // Assert
        assertEquals(saved, dao.findById(saved.getId()).orElseThrow());
        assertTrue(dao.findById(999L).isEmpty());
        assertTrue(all.contains(saved));
        assertThrows(UnsupportedOperationException.class, () -> all.add(training(2L)));
    }

    private Training training(Long id) {
        return new Training(id, 1L, 1L, "Training", TrainingType.STRENGTH,
                LocalDateTime.of(2025, 1, 1, 10, 0), 60L);
    }
}
