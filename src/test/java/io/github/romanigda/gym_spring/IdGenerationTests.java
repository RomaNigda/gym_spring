package io.github.romanigda.gym_spring;

import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.dao.impl.TraineeDaoImpl;
import io.github.romanigda.gym_spring.dao.impl.TrainerDaoImpl;
import io.github.romanigda.gym_spring.dao.impl.TrainingDaoImpl;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.service.TrainerService;
import io.github.romanigda.gym_spring.service.TrainingService;
import io.github.romanigda.gym_spring.storage.Storage;
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
        // Arrange
        Storage storage = new Storage(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new DefaultResourceLoader());
        TraineeDao traineeDao = new TraineeDaoImpl(storage);
        TrainerDao trainerDao = new TrainerDaoImpl(storage);
        TrainingDao trainingDao = new TrainingDaoImpl(storage);

        // Act
        Trainee savedTrainee = traineeDao.save(trainee("First"));
        Trainer savedTrainer = trainerDao.save(trainer("First"));
        Training savedTraining = trainingDao.save(training());

        // Assert
        assertEquals(1L, savedTrainee.getUserId());
        assertEquals(1L, savedTrainer.getUserId());
        assertEquals(1L, savedTraining.getId());
        assertIdsMatchKeys(storage);
    }

    @Test
    void deletedHighestIdsAreNotReused() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            TraineeDao traineeDao = context.getBean(TraineeDao.class);
            TrainerDao trainerDao = context.getBean(TrainerDao.class);
            TrainingDao trainingDao = context.getBean(TrainingDao.class);

            // Act
            traineeDao.delete(5L);
            trainerDao.delete(5L);
            trainingDao.delete(5L);
            Trainee sixthTrainee = traineeDao.save(trainee("One"));
            Trainer sixthTrainer = trainerDao.save(trainer("One"));
            Training sixthTraining = trainingDao.save(training());

            traineeDao.delete(6L);
            trainerDao.delete(6L);
            trainingDao.delete(6L);
            Trainee seventhTrainee = traineeDao.save(trainee("Two"));
            Trainer seventhTrainer = trainerDao.save(trainer("Two"));
            Training seventhTraining = trainingDao.save(training());

            // Assert
            assertEquals(6L, sixthTrainee.getUserId());
            assertEquals(6L, sixthTrainer.getUserId());
            assertEquals(6L, sixthTraining.getId());
            assertEquals(7L, seventhTrainee.getUserId());
            assertEquals(7L, seventhTrainer.getUserId());
            assertEquals(7L, seventhTraining.getId());
            assertIdsMatchKeys(context.getBean(Storage.class));
        }
    }

    @Test
    void generatedIdsAccountForExistingSparseMapKeys() {
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            Storage storage = context.getBean(Storage.class);
            storage.getTraineeMap().put(40L, new Trainee("Existing", "Trainee", "existing.trainee",
                    "password", true, DATE_OF_BIRTH, "address", 40L));
            storage.getTrainerMap().put(50L, new Trainer("Existing", "Trainer", "existing.trainer",
                    "password", true, Set.of(TrainingType.STRENGTH), 50L));
            storage.getTrainingMap().put(60L, new Training(60L, 1L, 1L, "Existing",
                    TrainingType.STRENGTH, TRAINING_DATE, 60L));

            // Act
            Trainee nextTrainee = context.getBean(TraineeService.class).create(trainee("Next"));
            Trainer nextTrainer = context.getBean(TrainerService.class).create(trainer("Next"));
            Training nextTraining = context.getBean(TrainingService.class).create(training());

            // Assert
            assertEquals(41L, nextTrainee.getUserId());
            assertEquals(51L, nextTrainer.getUserId());
            assertEquals(61L, nextTraining.getId());
            assertIdsMatchKeys(storage);
        }
    }

    @Test
    void seededProfilesCanBeUpdatedByTheirMapIds() {
        // Arrange
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

            // Act
            traineeService.update(updatedTrainee);
            trainerService.update(updatedTrainer);
            Trainee resultTrainee = traineeService.selectById(3L);
            Trainer resultTrainer = trainerService.selectById(3L);

            // Assert
            assertEquals("updated address", resultTrainee.getAddress());
            assertEquals(Set.of(TrainingType.BALANCE), resultTrainer.getTrainingTypes());
            assertIdsMatchKeys(context.getBean(Storage.class));
        }
    }

    @Test
    void concurrentCreationsUseDistinctIdsInEachMap() throws Exception {
        // Arrange
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

            // Act
            List<Boolean> taskResults = new ArrayList<>();
            for (var result : executor.invokeAll(tasks)) {
                taskResults.add(result.get());
            }
            Set<Long> expectedIds = LongStream.rangeClosed(6, 5 + countPerType)
                    .boxed().collect(Collectors.toSet());

            // Assert
            assertTrue(taskResults.stream().allMatch(Boolean::booleanValue),
                    "generated IDs must be unique within each map");
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
        // Arrange
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            Storage storage = context.getBean(Storage.class);
            storage.getTrainingMap().put(Long.MAX_VALUE, new Training(Long.MAX_VALUE, 1L, 1L, "Last",
                    TrainingType.STRENGTH, TRAINING_DATE, 60L));
            int sizeBefore = storage.getTrainingMap().size();

            // Act
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> context.getBean(TrainingService.class).create(training()));

            // Assert
            assertTrue(exception.getMessage().contains("maximum value"));
            assertEquals(sizeBefore, storage.getTrainingMap().size());
            assertIdsMatchKeys(storage);
        }
    }

    @Test
    void initializationRejectsProfileIdsThatDifferFromMapKeys() {
        // Arrange
        List<BeanCreationException> exceptions = new ArrayList<>();
        for (String file : List.of("invalid-trainee-ids.txt", "invalid-trainer-ids.txt")) {
            try (var context = new AnnotationConfigApplicationContext()) {
                context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                        "testStorage", Map.of("storage.init.file", "classpath:" + file)));
                context.register(AppConfig.class);

                // Act
                exceptions.add(assertThrows(BeanCreationException.class, context::refresh));
            }
        }

        // Assert
        assertEquals(2, exceptions.size());
        for (BeanCreationException exception : exceptions) {
            assertTrue(exception.getMostSpecificCause().getMessage()
                    .contains("Profile id 1 does not match userId 3"));
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
