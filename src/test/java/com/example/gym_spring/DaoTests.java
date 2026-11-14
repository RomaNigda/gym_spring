package com.example.gym_spring;

import com.example.gym_spring.config.AppConfig;
import com.example.gym_spring.dao.TraineeDao;
import com.example.gym_spring.dao.TrainerDao;
import com.example.gym_spring.dao.TrainingDao;
import com.example.gym_spring.domain.Trainee;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.domain.Training;
import com.example.gym_spring.domain.TrainingType;
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
            dao.save(trainer);

            assertEquals(trainer, dao.findById(101L).orElseThrow());
            assertEquals(trainer, dao.findByUsername("trainer.one").orElseThrow());
            assertTrue(dao.findAll().contains(trainer));
            assertThrows(IllegalArgumentException.class, () -> dao.save(trainer));

            Trainer updated = trainer(101L, "trainer.updated");
            dao.update(updated);
            assertEquals(updated, dao.findByUsername("trainer.updated").orElseThrow());
            assertThrows(IllegalArgumentException.class, () -> dao.update(trainer(999L, "missing")));
            assertThrows(IllegalArgumentException.class, () -> dao.delete(999L));

            dao.delete(101L);
            assertTrue(dao.findById(101L).isEmpty());
        }
    }

    @Test
    void traineeDaoSupportsCrudAndUsernameLookup() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeDao dao = context.getBean(TraineeDao.class);
            Trainee trainee = trainee(101L, "trainee.one");
            dao.save(trainee);

            assertEquals(trainee, dao.findById(101L).orElseThrow());
            assertEquals(trainee, dao.findByUsername("trainee.one").orElseThrow());
            assertTrue(dao.findAll().contains(trainee));

            Trainee updated = trainee(101L, "trainee.updated");
            dao.update(updated);
            assertEquals(updated, dao.findByUsername("trainee.updated").orElseThrow());
            assertThrows(IllegalArgumentException.class, () -> dao.update(trainee(999L, "missing")));
            assertThrows(IllegalArgumentException.class, () -> dao.delete(999L));

            dao.delete(101L);
            assertFalse(dao.findById(101L).isPresent());
        }
    }

    @Test
    void trainingDaoUsesTrainingIdForCrud() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TrainingDao dao = context.getBean(TrainingDao.class);
            Training training = training(101L);
            dao.save(training);

            assertEquals(training, dao.findById(101L).orElseThrow());
            assertTrue(dao.findAll().contains(training));
            assertThrows(IllegalArgumentException.class, () -> dao.save(training));

            Training updated = training(101L);
            dao.update(updated);
            assertEquals(updated, dao.findById(101L).orElseThrow());
            assertThrows(IllegalArgumentException.class, () -> dao.update(training(999L)));
            assertThrows(IllegalArgumentException.class, () -> dao.delete(999L));

            dao.delete(101L);
            assertTrue(dao.findById(101L).isEmpty());
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
