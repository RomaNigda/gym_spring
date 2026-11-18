package io.github.romanigda.gym_spring;

import io.github.romanigda.gym_spring.storage.Storage;
import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.facade.GymFacade;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GymSpringApplicationTests {

	@Test
	void facadeIsWiredAndExposesInitialData() {
		try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			GymFacade facade = context.getBean(GymFacade.class);
			assertEquals(5, facade.selectAllTrainees().size());
			assertEquals(5, facade.selectAllTrainers().size());
			assertEquals(5, facade.selectAllTrainings().size());
		}
	}

	@Test
	void facadeCreatesAndSelectsLinkedEntitiesAndPropagatesDomainErrors() {
		try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			GymFacade facade = context.getBean(GymFacade.class);
			Trainee trainee = facade.createTrainee(new Trainee("Facade", "Trainee", true,
					LocalDate.of(1990, 1, 1), "address"));
			Trainer trainer = facade.createTrainer(new Trainer("Facade", "Trainer", true,
					Set.of(TrainingType.STRENGTH)));
			Training training = facade.createTraining(new Training(
					trainee.getUserId(), trainer.getUserId(), "Facade session", TrainingType.STRENGTH,
					LocalDateTime.of(2026, 12, 1, 10, 0), 60L));

			assertEquals(6L, trainee.getUserId());
			assertEquals(6L, trainer.getUserId());
			assertEquals(6L, training.getId());
			assertEquals(trainee, facade.selectTraineeById(training.getTraineeId()));
			assertEquals(trainer, facade.selectTrainerById(training.getTrainerId()));
			assertEquals(trainee, facade.selectTraineeByUsername(trainee.getUsername()));
			assertEquals(trainer, facade.selectTrainerByUsername(trainer.getUsername()));
			assertEquals(training, facade.selectTrainingById(training.getId()));
			assertTrue(trainee.getPassword().matches("[A-Za-z0-9]{10}"));
			assertTrue(trainer.getPassword().matches("[A-Za-z0-9]{10}"));
			assertThrows(EntityNotFoundException.class, () -> facade.selectTraineeById(999L));
			assertThrows(EntityNotFoundException.class, () -> facade.selectTrainerById(999L));
			assertThrows(EntityNotFoundException.class, () -> facade.selectTrainingById(999L));
			assertThrows(ValidationException.class, () -> facade.createTrainee(null));
			assertThrows(ValidationException.class, () -> facade.createTrainer(null));
			assertThrows(ValidationException.class, () -> facade.createTraining(null));
		}
	}

	@Test
	void loadsInitialDataIntoStorageMaps() {
		try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			Storage storage = context.getBean(Storage.class);
			assertEquals(5, storage.getTrainerMap().size());
			assertEquals(5, storage.getTraineeMap().size());
			assertEquals(5, storage.getTrainingMap().size());
			storage.getTrainerMap().forEach((id, trainer) -> assertEquals(id, trainer.getUserId()));
			storage.getTraineeMap().forEach((id, trainee) -> assertEquals(id, trainee.getUserId()));
			storage.getTrainingMap().forEach((id, training) -> assertEquals(id, training.getId()));
			storage.getTrainingMap().values().forEach(training -> {
				assertTrue(storage.getTraineeMap().containsKey(training.getTraineeId()));
				assertTrue(storage.getTrainerMap().containsKey(training.getTrainerId()));
			});
		}
	}

}
