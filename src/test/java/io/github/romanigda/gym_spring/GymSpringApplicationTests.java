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
		// Arrange
		try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			GymFacade facade = context.getBean(GymFacade.class);

			// Act
			int traineeCount = facade.selectAllTrainees().size();
			int trainerCount = facade.selectAllTrainers().size();
			int trainingCount = facade.selectAllTrainings().size();

			// Assert
			assertEquals(5, traineeCount);
			assertEquals(5, trainerCount);
			assertEquals(5, trainingCount);
		}
	}

	@Test
	void facadeCreatesAndSelectsLinkedEntitiesAndPropagatesDomainErrors() {
		// Arrange
		try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			GymFacade facade = context.getBean(GymFacade.class);

			// Act
			Trainee trainee = facade.createTrainee(new Trainee("Facade", "Trainee", true,
					LocalDate.of(1990, 1, 1), "address"));
			Trainer trainer = facade.createTrainer(new Trainer("Facade", "Trainer", true,
					Set.of(TrainingType.STRENGTH)));
			Training training = facade.createTraining(new Training(
					trainee.getUserId(), trainer.getUserId(), "Facade session", TrainingType.STRENGTH,
					LocalDateTime.of(2026, 12, 1, 10, 0), 60L));
			Trainee selectedTraineeById = facade.selectTraineeById(training.getTraineeId());
			Trainer selectedTrainerById = facade.selectTrainerById(training.getTrainerId());
			Trainee selectedTraineeByUsername = facade.selectTraineeByUsername(trainee.getUsername());
			Trainer selectedTrainerByUsername = facade.selectTrainerByUsername(trainer.getUsername());
			Training selectedTraining = facade.selectTrainingById(training.getId());
			EntityNotFoundException traineeNotFound = assertThrows(EntityNotFoundException.class,
					() -> facade.selectTraineeById(999L));
			EntityNotFoundException trainerNotFound = assertThrows(EntityNotFoundException.class,
					() -> facade.selectTrainerById(999L));
			EntityNotFoundException trainingNotFound = assertThrows(EntityNotFoundException.class,
					() -> facade.selectTrainingById(999L));
			ValidationException traineeValidation = assertThrows(ValidationException.class,
					() -> facade.createTrainee(null));
			ValidationException trainerValidation = assertThrows(ValidationException.class,
					() -> facade.createTrainer(null));
			ValidationException trainingValidation = assertThrows(ValidationException.class,
					() -> facade.createTraining(null));

			// Assert
			assertEquals(6L, trainee.getUserId());
			assertEquals(6L, trainer.getUserId());
			assertEquals(6L, training.getId());
			assertEquals(trainee, selectedTraineeById);
			assertEquals(trainer, selectedTrainerById);
			assertEquals(trainee, selectedTraineeByUsername);
			assertEquals(trainer, selectedTrainerByUsername);
			assertEquals(training, selectedTraining);
			assertTrue(trainee.getPassword().matches("[A-Za-z0-9]{10}"));
			assertTrue(trainer.getPassword().matches("[A-Za-z0-9]{10}"));
			assertTrue(traineeNotFound.getMessage().contains("999"));
			assertTrue(trainerNotFound.getMessage().contains("999"));
			assertTrue(trainingNotFound.getMessage().contains("999"));
			assertTrue(traineeValidation.getMessage().contains("must not be null"));
			assertTrue(trainerValidation.getMessage().contains("must not be null"));
			assertTrue(trainingValidation.getMessage().contains("must not be null"));
		}
	}

	@Test
	void loadsInitialDataIntoStorageMaps() {
		// Arrange
		try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			Storage storage = context.getBean(Storage.class);

			// Act
			int trainerCount = storage.getTrainerMap().size();
			int traineeCount = storage.getTraineeMap().size();
			int trainingCount = storage.getTrainingMap().size();

			// Assert
			assertEquals(5, trainerCount);
			assertEquals(5, traineeCount);
			assertEquals(5, trainingCount);
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
