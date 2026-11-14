package com.example.gym_spring;

import com.example.gym_spring.storage.Storage;
import com.example.gym_spring.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GymSpringApplicationTests {

	@Test
	void loadsInitialDataIntoStorageMaps() {
		try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			Storage storage = context.getBean(Storage.class);
			assertEquals(5, storage.getTrainerMap().size());
			assertEquals(5, storage.getTraineeMap().size());
			assertEquals(5, storage.getTrainingMap().size());
			storage.getTrainingMap().forEach((id, training) -> assertEquals(id, training.getId()));
		}
	}

}
