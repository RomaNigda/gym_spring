package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeDaoImplTest {
    private TraineeDaoImpl dao;

    @BeforeEach
    void setUp() {
        Storage storage = new Storage(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new DefaultResourceLoader());
        dao = new TraineeDaoImpl(storage);
    }

    @Test
    void saveGeneratesAnIdAndStoresTheTrainee() {
        // Arrange
        Trainee trainee = trainee(101L, "trainee.one");

        // Act
        Trainee saved = dao.save(trainee);

        // Assert
        assertEquals(1L, saved.getUserId());
        assertEquals("trainee.one", dao.findById(saved.getUserId()).orElseThrow().getUsername());
        assertEquals(1990, saved.getDateOfBirth().getYear());
    }

    @Test
    void updateReplacesAnExistingTrainee() {
        // Arrange
        Trainee saved = dao.save(trainee(101L, "trainee.one"));
        Trainee updated = trainee(saved.getUserId(), "trainee.updated");

        // Act
        dao.update(updated);

        // Assert
        assertEquals(updated, dao.findById(saved.getUserId()).orElseThrow());
        assertEquals(updated, dao.findByUsername("trainee.updated").orElseThrow());
        assertTrue(dao.findByUsername("trainee.one").isEmpty());
    }

    @Test
    void updateThrowsWhenTheTraineeDoesNotExist() {
        // Arrange
        Trainee missing = trainee(999L, "missing");

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> dao.update(missing));

        // Assert
        assertEquals("Trainee not found: 999", exception.getMessage());
    }

    @Test
    void deleteRemovesAnExistingTrainee() {
        // Arrange
        Trainee saved = dao.save(trainee(101L, "trainee.one"));

        // Act
        dao.delete(saved.getUserId());

        // Assert
        assertTrue(dao.findById(saved.getUserId()).isEmpty());
        assertTrue(dao.findByUsername("trainee.one").isEmpty());
    }

    @Test
    void deleteThrowsWhenTheTraineeDoesNotExist() {
        // Arrange
        Long missingId = 999L;

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> dao.delete(missingId));

        // Assert
        assertEquals("Trainee not found: 999", exception.getMessage());
    }

    @Test
    void findMethodsReturnMatchingTraineesAndFindAllIsReadOnly() {
        // Arrange
        Trainee saved = dao.save(trainee(101L, "trainee.one"));

        // Act
        var all = dao.findAll();

        // Assert
        assertEquals(saved, dao.findById(saved.getUserId()).orElseThrow());
        assertEquals(saved, dao.findByUsername("trainee.one").orElseThrow());
        assertTrue(dao.findById(999L).isEmpty());
        assertTrue(dao.findByUsername("missing").isEmpty());
        assertTrue(all.contains(saved));
        assertThrows(UnsupportedOperationException.class, () -> all.add(trainee(2L, "another")));
        assertFalse(all.isEmpty());
    }

    private Trainee trainee(Long id, String username) {
        return new Trainee("Trainee", "Test", username, "password", true,
                LocalDate.of(1990, 1, 1), "address", id);
    }
}
