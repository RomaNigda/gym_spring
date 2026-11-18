package io.github.romanigda.gym_spring;

import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaoTests {

    @Test
    void trainerDaoSupportsCrudAndUsernameLookup() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainerDao dao = context.getBean(TrainerDao.class);
            Trainer trainer = trainer(101L, "trainer.one");
            Trainer saved = dao.save(trainer);

            assertEquals(6L, saved.getUserId());
            assertEquals(saved, dao.findById(saved.getUserId()).orElseThrow());
            assertEquals(saved, dao.findByUsername("trainer.one").orElseThrow());
            assertTrue(dao.findAll().contains(saved));
            assertEquals(7L, dao.save(trainer).getUserId());

            Trainer updated = trainer(saved.getUserId(), "trainer.updated");
            dao.update(updated);
            assertEquals(updated, dao.findByUsername("trainer.updated").orElseThrow());
            assertThrows(IllegalArgumentException.class, () -> dao.update(trainer(999L, "missing")));
            assertThrows(IllegalArgumentException.class, () -> dao.delete(999L));

            dao.delete(saved.getUserId());
            assertTrue(dao.findById(saved.getUserId()).isEmpty());
        }
    }

    @Test
    void traineeDaoSupportsCrudAndUsernameLookup() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeDao dao = context.getBean(TraineeDao.class);
            Trainee trainee = trainee(101L, "trainee.one");
            Trainee saved = dao.save(trainee);

            assertEquals(6L, saved.getUserId());
            assertEquals(saved, dao.findById(saved.getUserId()).orElseThrow());
            assertEquals(saved, dao.findByUsername("trainee.one").orElseThrow());
            assertTrue(dao.findAll().contains(saved));
            assertEquals(7L, dao.save(trainee).getUserId());

            Trainee updated = trainee(saved.getUserId(), "trainee.updated");
            dao.update(updated);
            assertEquals(updated, dao.findByUsername("trainee.updated").orElseThrow());
            assertThrows(IllegalArgumentException.class, () -> dao.update(trainee(999L, "missing")));
            assertThrows(IllegalArgumentException.class, () -> dao.delete(999L));

            dao.delete(saved.getUserId());
            assertFalse(dao.findById(saved.getUserId()).isPresent());
        }
    }

    @Test
    void trainingDaoUsesTrainingIdForCrud() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainingDao dao = context.getBean(TrainingDao.class);
            Training training = training(101L);
            Training saved = dao.save(training);

            assertEquals(6L, saved.getId());
            assertEquals(saved, dao.findById(saved.getId()).orElseThrow());
            assertTrue(dao.findAll().contains(saved));
            assertEquals(7L, dao.save(training).getId());

            Training updated = training(saved.getId());
            dao.update(updated);
            assertEquals(updated, dao.findById(saved.getId()).orElseThrow());
            assertThrows(IllegalArgumentException.class, () -> dao.update(training(999L)));
            assertThrows(IllegalArgumentException.class, () -> dao.delete(999L));

            dao.delete(saved.getId());
            assertTrue(dao.findById(saved.getId()).isEmpty());
        }
    }

    private Trainer trainer(Long id, String username) {
        return new Trainer("Trainer", "Test", username, "password", true, Set.of(TrainingType.STRENGTH), id);
    }

    private Trainee trainee(Long id, String username) {
        return new Trainee("Trainee", "Test", username, "password", true,
                LocalDate.of(1990, 1, 1), "address", id);
    }

    private Training training(Long id) {
        return new Training(id, 1L, 1L, "Training", TrainingType.STRENGTH,
                LocalDateTime.of(2025, 1, 1, 10, 0), 60L);
    }
}
