package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.TrainingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingTypeDaoImplTest {
    private DaoTestSupport db;
    private TrainingTypeDaoImpl dao;
    private TrainingType yoga;

    @BeforeEach
    void setUp() {
        db = new DaoTestSupport();
        dao = new TrainingTypeDaoImpl(db.sessionFactory());
        db.inTransaction(() -> {
            yoga = db.persistTrainingType("YOGA");
            db.persistTrainingType("CARDIO");
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void findByIdReturnsTrainingType() {
        // Arrange
        Long id = yoga.getId();

        // Act
        Optional<TrainingType> result = db.inTransaction(() -> dao.findById(id));

        // Assert
        assertEquals("YOGA", result.orElseThrow().getTrainingTypeName());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        // Arrange
        Long id = 9999L;

        // Act
        Optional<TrainingType> result = db.inTransaction(() -> dao.findById(id));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByNameIsCaseInsensitiveAndTrimsInput() {
        // Arrange
        String name = "  yOgA ";

        // Act
        Optional<TrainingType> result = db.inTransaction(() -> dao.findByName(name));

        // Assert
        assertEquals(yoga.getId(), result.orElseThrow().getId());
    }

    @Test
    void findByNameReturnsEmptyWhenMissing() {
        // Arrange
        String name = "BOXING";

        // Act
        Optional<TrainingType> result = db.inTransaction(() -> dao.findByName(name));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findAllReturnsAllTypesOrderedById() {
        // Arrange
        // (YOGA and CARDIO are persisted in setUp)

        // Act
        List<TrainingType> result = db.inTransaction(() -> dao.findAll());

        // Assert
        assertEquals(List.of("YOGA", "CARDIO"), result.stream().map(TrainingType::getTrainingTypeName).toList());
    }
}
