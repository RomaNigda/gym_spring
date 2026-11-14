package com.example.gym_spring;

import com.example.gym_spring.storage.Storage;
import com.example.gym_spring.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GymSpringApplicationTests {

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
