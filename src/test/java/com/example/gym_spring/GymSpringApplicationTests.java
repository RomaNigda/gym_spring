package com.example.gym_spring;

import com.example.gym_spring.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class GymSpringApplicationTests {

	@Autowired
	private Storage storage;

	@Test
	void loadsInitialDataIntoStorageMaps() {
		assertEquals(5, storage.getTrainerMap().size());
		assertEquals(5, storage.getTraineeMap().size());
		assertEquals(5, storage.getTrainingMap().size());
	}

}
