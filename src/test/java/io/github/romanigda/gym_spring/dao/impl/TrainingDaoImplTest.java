package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDaoImplTest {
    private DaoTestSupport db;
    private TrainingDaoImpl dao;

    @BeforeEach
    void setUp() {
        db = new DaoTestSupport();
        dao = new TrainingDaoImpl(db.sessionFactory());
        db.inTransaction(() -> {
            TrainingType yoga = db.persistTrainingType("YOGA");
            TrainingType cardio = db.persistTrainingType("CARDIO");
            Trainee john = db.persistTrainee("John", "Doe", "john.doe");
            Trainee mary = db.persistTrainee("Mary", "Major", "mary.major");
            Trainer jane = db.persistTrainer("Jane", "Roe", "jane.roe", yoga);
            Trainer bob = db.persistTrainer("Bob", "Smith", "bob.smith", cardio);
            db.persistTraining(john, bob, "Late", cardio, LocalDate.of(2025, 3, 1));
            db.persistTraining(john, jane, "Early", yoga, LocalDate.of(2025, 1, 1));
            db.persistTraining(john, jane, "Middle", yoga, LocalDate.of(2025, 2, 1));
            db.persistTraining(mary, jane, "Other", yoga, LocalDate.of(2025, 2, 15));
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private List<String> traineeNames(String username, LocalDate from, LocalDate to, String trainer, String type) {
        return db.inTransaction(() -> dao.findTraineeTrainings(username, from, to, trainer, type)).stream()
                .map(Training::getTrainingName).toList();
    }

    private List<String> trainerNames(String username, LocalDate from, LocalDate to, String trainee) {
        return db.inTransaction(() -> dao.findTrainerTrainings(username, from, to, trainee)).stream()
                .map(Training::getTrainingName).toList();
    }

    @Test
    void savePersistsTraining() {
        // Arrange
        Training training = db.inTransaction(() -> {
            Trainee trainee = db.session().createQuery("from Trainee where username = 'mary.major'", Trainee.class)
                    .getSingleResult();
            Trainer trainer = db.session().createQuery("from Trainer where username = 'bob.smith'", Trainer.class)
                    .getSingleResult();
            return new Training(trainee, trainer, "New", trainer.getSpecialization(), LocalDate.of(2025, 4, 1), 45);
        });

        // Act
        Training saved = db.inTransaction(() -> dao.save(training));

        // Assert
        assertNotNull(saved.getId());
        assertEquals(List.of("Other", "New"), traineeNames("mary.major", null, null, null, null));
    }

    @Test
    void findTraineeTrainingsWithoutFiltersReturnsAllOrderedByDate() {
        // Arrange
        // (trainings are persisted in setUp)

        // Act
        List<String> result = traineeNames("john.doe", null, null, null, null);

        // Assert
        assertEquals(List.of("Early", "Middle", "Late"), result);
    }

    @Test
    void findTraineeTrainingsFiltersByDateRangeInclusively() {
        // Arrange
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to = LocalDate.of(2025, 3, 1);

        // Act
        List<String> result = traineeNames("john.doe", from, to, null, null);

        // Assert
        assertEquals(List.of("Middle", "Late"), result);
    }

    @Test
    void findTraineeTrainingsFiltersByFromDateOnly() {
        // Arrange
        LocalDate from = LocalDate.of(2025, 2, 2);

        // Act
        List<String> result = traineeNames("john.doe", from, null, null, null);

        // Assert
        assertEquals(List.of("Late"), result);
    }

    @Test
    void findTraineeTrainingsFiltersByToDateOnly() {
        // Arrange
        LocalDate to = LocalDate.of(2025, 1, 31);

        // Act
        List<String> result = traineeNames("john.doe", null, to, null, null);

        // Assert
        assertEquals(List.of("Early"), result);
    }

    @Test
    void findTraineeTrainingsFiltersByTrainerNameOrUsernameIgnoringCase() {
        // Arrange
        // (john has trainings with jane.roe and bob.smith)

        // Act
        List<String> byFullName = traineeNames("john.doe", null, null, "  BOB sm ", null);
        List<String> byUsername = traineeNames("john.doe", null, null, "jane.r", null);

        // Assert
        assertEquals(List.of("Late"), byFullName);
        assertEquals(List.of("Early", "Middle"), byUsername);
    }

    @Test
    void findTraineeTrainingsFiltersByTrainingTypeIgnoringCase() {
        // Arrange
        String type = " cardio ";

        // Act
        List<String> result = traineeNames("john.doe", null, null, null, type);

        // Assert
        assertEquals(List.of("Late"), result);
    }

    @Test
    void findTraineeTrainingsIgnoresBlankOptionalFilters() {
        // Arrange
        // (blank strings mean "do not filter")

        // Act
        List<String> result = traineeNames("john.doe", null, null, "  ", "");

        // Assert
        assertEquals(3, result.size());
    }

    @Test
    void findTraineeTrainingsCombinesFilters() {
        // Arrange
        LocalDate from = LocalDate.of(2025, 1, 15);

        // Act
        List<String> result = traineeNames("john.doe", from, null, "jane", "yoga");

        // Assert
        assertEquals(List.of("Middle"), result);
    }

    @Test
    void findTraineeTrainingsReturnsEmptyForUnknownTrainee() {
        // Arrange
        String username = "nobody";

        // Act
        List<String> result = traineeNames(username, null, null, null, null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findTrainerTrainingsWithoutFiltersReturnsAllOrderedByDate() {
        // Arrange
        // (jane trains john twice and mary once)

        // Act
        List<String> result = trainerNames("jane.roe", null, null, null);

        // Assert
        assertEquals(List.of("Early", "Middle", "Other"), result);
    }

    @Test
    void findTrainerTrainingsFiltersByDateRange() {
        // Arrange
        LocalDate from = LocalDate.of(2025, 1, 15);
        LocalDate to = LocalDate.of(2025, 2, 10);

        // Act
        List<String> result = trainerNames("jane.roe", from, to, null);

        // Assert
        assertEquals(List.of("Middle"), result);
    }

    @Test
    void findTrainerTrainingsFiltersByTraineeNameIgnoringCase() {
        // Arrange
        String traineeName = "MARY major";

        // Act
        List<String> result = trainerNames("jane.roe", null, null, traineeName);

        // Assert
        assertEquals(List.of("Other"), result);
    }

    @Test
    void findTrainerTrainingsReturnsEmptyForUnknownTrainer() {
        // Arrange
        String username = "nobody";

        // Act
        List<String> result = trainerNames(username, null, null, null);

        // Assert
        assertTrue(result.isEmpty());
    }
}
