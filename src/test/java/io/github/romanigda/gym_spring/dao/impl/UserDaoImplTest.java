package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoImplTest {
    private DaoTestSupport db;
    private UserDaoImpl dao;

    @BeforeEach
    void setUp() {
        db = new DaoTestSupport();
        dao = new UserDaoImpl(db.sessionFactory());
        db.inTransaction(() -> {
            db.persistTrainee("John", "Doe", "john.doe");
            db.persistTrainee("John", "Doe", "john.doe1");
            db.persistTrainee("John", "Dollar", "john.dollar");
            db.persistTrainee("Under", "Score", "a_b.c");
            db.persistTrainee("Other", "Name", "axb.c");
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void findByUsernameReturnsUser() {
        // Arrange
        String username = "john.doe";

        // Act
        Optional<User> result = db.inTransaction(() -> dao.findByUsername(username));

        // Assert
        assertInstanceOf(io.github.romanigda.gym_spring.domain.Trainee.class, result.orElseThrow());
    }

    @Test
    void findByUsernameReturnsEmptyWhenMissing() {
        // Arrange
        String username = "missing";

        // Act
        Optional<User> result = db.inTransaction(() -> dao.findByUsername(username));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUsernameReflectsPresence() {
        // Arrange
        // (users are persisted in setUp)

        // Act
        boolean exists = db.inTransaction(() -> dao.existsByUsername("john.doe"));
        boolean missing = db.inTransaction(() -> dao.existsByUsername("nobody"));

        // Assert
        assertTrue(exists);
        assertFalse(missing);
    }

    @Test
    void findUsernamesStartingWithReturnsMatchingPrefixes() {
        // Arrange
        String prefix = "john.do";

        // Act
        List<String> result = db.inTransaction(() -> dao.findUsernamesStartingWith(prefix));

        // Assert
        assertEquals(Set.of("john.doe", "john.doe1", "john.dollar"), Set.copyOf(result));
    }

    @Test
    void findUsernamesStartingWithTreatsLikeWildcardsLiterally() {
        // Arrange
        String prefix = "a_b";

        // Act
        List<String> result = db.inTransaction(() -> dao.findUsernamesStartingWith(prefix));

        // Assert
        assertEquals(List.of("a_b.c"), result);
    }

    @Test
    void findUsernamesStartingWithReturnsEmptyWhenNothingMatches() {
        // Arrange
        String prefix = "zzz";

        // Act
        List<String> result = db.inTransaction(() -> dao.findUsernamesStartingWith(prefix));

        // Assert
        assertTrue(result.isEmpty());
    }
}
