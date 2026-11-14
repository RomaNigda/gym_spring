package com.example.gym_spring;

import com.example.gym_spring.config.AppConfig;
import com.example.gym_spring.dao.TraineeDao;
import com.example.gym_spring.dao.TrainerDao;
import com.example.gym_spring.dao.TrainingDao;
import com.example.gym_spring.dao.impl.TraineeDaoImpl;
import com.example.gym_spring.dao.impl.TrainerDaoImpl;
import com.example.gym_spring.dao.impl.TrainingDaoImpl;
import com.example.gym_spring.domain.Trainee;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.domain.Training;
import com.example.gym_spring.domain.TrainingType;
import com.example.gym_spring.service.TraineeService;
import com.example.gym_spring.service.TrainerService;
import com.example.gym_spring.service.TrainingService;
import com.example.gym_spring.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdGenerationTests {
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 1, 1);
    private static final LocalDateTime TRAINING_DATE = LocalDateTime.of(2026, 12, 1, 10, 0);

    @Test
    void emptyMapsStartAtOne() {
        Storage storage = new Storage(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new DefaultResourceLoader());
        TraineeDao traineeDao = new TraineeDaoImpl(storage);
        TrainerDao trainerDao = new TrainerDaoImpl(storage);
        TrainingDao trainingDao = new TrainingDaoImpl(storage);

        assertEquals(1L, traineeDao.save(trainee("First")).getUserId());
        assertEquals(1L, trainerDao.save(trainer("First")).getUserId());
        assertEquals(1L, trainingDao.save(training()).getId());
        assertIdsMatchKeys(storage);
    }

    @Test
    void deletedHighestIdsAreNotReused() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeDao traineeDao = context.getBean(TraineeDao.class);
            TrainerDao trainerDao = context.getBean(TrainerDao.class);
            TrainingDao trainingDao = context.getBean(TrainingDao.class);

            traineeDao.delete(5L);
            trainerDao.delete(5L);
            trainingDao.delete(5L);
            assertEquals(6L, traineeDao.save(trainee("One")).getUserId());
            assertEquals(6L, trainerDao.save(trainer("One")).getUserId());
            assertEquals(6L, trainingDao.save(training()).getId());

            traineeDao.delete(6L);
            trainerDao.delete(6L);
            trainingDao.delete(6L);
            assertEquals(7L, traineeDao.save(trainee("Two")).getUserId());
            assertEquals(7L, trainerDao.save(trainer("Two")).getUserId());
            assertEquals(7L, trainingDao.save(training()).getId());
            assertIdsMatchKeys(context.getBean(Storage.class));
        }
    }

    @Test
    void generatedIdsAccountForExistingSparseMapKeys() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            Storage storage = context.getBean(Storage.class);
            storage.getTraineeMap().put(40L, new Trainee("Existing", "Trainee", "existing.trainee",
                    "password", true, DATE_OF_BIRTH, "address", 40L));
            storage.getTrainerMap().put(50L, new Trainer("Existing", "Trainer", "existing.trainer",
                    "password", true, Set.of(TrainingType.STRENGTH), 50L));
            storage.getTrainingMap().put(60L, new Training(60L, 1L, 1L, "Existing",
                    TrainingType.STRENGTH, TRAINING_DATE, 60L));

            assertEquals(41L, context.getBean(TraineeService.class).create(trainee("Next")).getUserId());
            assertEquals(51L, context.getBean(TrainerService.class).create(trainer("Next")).getUserId());
            assertEquals(61L, context.getBean(TrainingService.class).create(training()).getId());
            assertIdsMatchKeys(storage);
        }
    }

    @Test
    void seededProfilesCanBeUpdatedByTheirMapIds() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeService traineeService = context.getBean(TraineeService.class);
            TrainerService trainerService = context.getBean(TrainerService.class);
            Trainee trainee = traineeService.selectById(3L);
            Trainer trainer = trainerService.selectById(3L);

            Trainee updatedTrainee = new Trainee(trainee.getFirstName(), trainee.getLastName(),
                    trainee.getUsername(), trainee.getPassword(), trainee.isActive(),
                    trainee.getDateOfBirth(), "updated address", trainee.getUserId());
            Trainer updatedTrainer = new Trainer(trainer.getFirstName(), trainer.getLastName(),
                    trainer.getUsername(), trainer.getPassword(), trainer.isActive(),
                    Set.of(TrainingType.BALANCE), trainer.getUserId());
            traineeService.update(updatedTrainee);
            trainerService.update(updatedTrainer);

            assertEquals("updated address", traineeService.selectById(3L).getAddress());
            assertEquals(Set.of(TrainingType.BALANCE), trainerService.selectById(3L).getTrainingTypes());
            assertIdsMatchKeys(context.getBean(Storage.class));
        }
    }

    @Test
    void concurrentCreationsUseDistinctIdsInEachMap() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class);
             var executor = Executors.newFixedThreadPool(8)) {
            TraineeService traineeService = context.getBean(TraineeService.class);
            TrainerService trainerService = context.getBean(TrainerService.class);
            TrainingService trainingService = context.getBean(TrainingService.class);
            Set<Long> traineeIds = ConcurrentHashMap.newKeySet();
            Set<Long> trainerIds = ConcurrentHashMap.newKeySet();
            Set<Long> trainingIds = ConcurrentHashMap.newKeySet();
            List<Callable<Boolean>> tasks = new ArrayList<>();
            int countPerType = 24;
            for (int i = 0; i < countPerType; i++) {
                String name = "Person" + i;
                tasks.add(() -> traineeIds.add(traineeService.create(trainee(name)).getUserId()));
                tasks.add(() -> trainerIds.add(trainerService.create(trainer(name)).getUserId()));
                tasks.add(() -> trainingIds.add(trainingService.create(training()).getId()));
            }

            for (var result : executor.invokeAll(tasks)) {
                assertTrue(result.get(), "generated IDs must be unique within each map");
            }
            Set<Long> expectedIds = LongStream.rangeClosed(6, 5 + countPerType)
                    .boxed().collect(Collectors.toSet());
            assertEquals(expectedIds, traineeIds);
            assertEquals(expectedIds, trainerIds);
            assertEquals(expectedIds, trainingIds);

            Storage storage = context.getBean(Storage.class);
            assertEquals(5 + countPerType, storage.getTraineeMap().size());
            assertEquals(5 + countPerType, storage.getTrainerMap().size());
            assertEquals(5 + countPerType, storage.getTrainingMap().size());
            assertIdsMatchKeys(storage);
        }
    }

    @Test
    void idExhaustionFailsInsteadOfWrappingAround() {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            Storage storage = context.getBean(Storage.class);
            storage.getTrainingMap().put(Long.MAX_VALUE, new Training(Long.MAX_VALUE, 1L, 1L, "Last",
                    TrainingType.STRENGTH, TRAINING_DATE, 60L));
            int sizeBefore = storage.getTrainingMap().size();

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> context.getBean(TrainingService.class).create(training()));
            assertTrue(exception.getMessage().contains("maximum value"));
            assertEquals(sizeBefore, storage.getTrainingMap().size());
            assertIdsMatchKeys(storage);
        }
    }

    @Test
    void initializationRejectsProfileIdsThatDifferFromMapKeys() {
        for (String file : List.of("invalid-trainee-ids.txt", "invalid-trainer-ids.txt")) {
            try (var context = new AnnotationConfigApplicationContext()) {
                context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                        "testStorage", Map.of("storage.init.file", "classpath:" + file)));
                context.register(AppConfig.class);

                BeanCreationException exception = assertThrows(BeanCreationException.class, context::refresh);
                assertTrue(exception.getMostSpecificCause().getMessage()
                        .contains("Profile id 1 does not match userId 3"));
            }
        }
    }

    private Trainee trainee(String firstName) {
        return new Trainee(firstName, "Trainee", true, DATE_OF_BIRTH, "address");
    }

    private Trainer trainer(String firstName) {
        return new Trainer(firstName, "Trainer", true, Set.of(TrainingType.STRENGTH));
    }

    private Training training() {
        return new Training(1L, 1L, "Training", TrainingType.STRENGTH, TRAINING_DATE, 60L);
    }

    private void assertIdsMatchKeys(Storage storage) {
        storage.getTraineeMap().forEach((id, trainee) -> assertEquals(id, trainee.getUserId()));
        storage.getTrainerMap().forEach((id, trainer) -> assertEquals(id, trainer.getUserId()));
        storage.getTrainingMap().forEach((id, training) -> assertEquals(id, training.getId()));
    }
}
