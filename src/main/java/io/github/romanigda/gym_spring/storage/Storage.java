package io.github.romanigda.gym_spring.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongFunction;

@Component
public class Storage {

    private static final Logger log = LoggerFactory.getLogger(Storage.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Getter
    private final Map<Long, Trainer> trainerMap;
    @Getter
    private final Map<Long, Trainee> traineeMap;
    @Getter
    private final Map<Long, Training> trainingMap;
    @Getter
    private final Map<Long, TrainingType> trainingTypeMap;

    // Keep a high-water mark so deleted IDs are not reused.
    private final AtomicLong trainerIdSequence = new AtomicLong();
    private final AtomicLong traineeIdSequence = new AtomicLong();
    private final AtomicLong trainingIdSequence = new AtomicLong();

    @Value("${storage.init.file}")
    private String initFilePath;

    private final ResourceLoader resourceLoader;

    public Storage(
            @Qualifier("trainerStorage") Map<Long, Trainer> trainerStorage,
            @Qualifier("traineeStorage") Map<Long, Trainee> traineeStorage,
            @Qualifier("trainingStorage") Map<Long, Training> trainingStorage,
            @Qualifier("trainingTypeStorage") Map<Long, TrainingType> trainingTypeStorage,
            ResourceLoader resourceLoader) {
        this.trainerMap = trainerStorage;
        this.traineeMap = traineeStorage;
        this.trainingMap = trainingStorage;
        this.trainingTypeMap = trainingTypeStorage;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        log.info("Loading initial data from {}", initFilePath);
        Resource resource = resourceLoader.getResource(initFilePath);
        int trainersLoaded = 0;
        int traineesLoaded = 0;
        int trainingsLoaded = 0;

        try (var inputStream = resource.getInputStream()) {
            InitializationData data = Objects.requireNonNull(
                    OBJECT_MAPPER.readValue(inputStream, InitializationData.class),
                    "Initialization JSON must contain an object");
            List<TrainerData> trainers = requireEntries(data.trainers(), "trainers");
            List<TraineeData> trainees = requireEntries(data.trainees(), "trainees");
            List<TrainingData> trainings = requireEntries(data.trainings(), "trainings");
            for (int i = 0; i < trainers.size(); i++) {
                TrainerData trainerData = trainers.get(i);
                try {
                    Trainer trainer = parseTrainer(trainerData);
                    putIfAbsent(trainerMap, trainerData.id(), trainer, "trainer");
                    trainersLoaded++;
                } catch (RuntimeException exception) {
                    throw invalidEntry("trainers", i, exception);
                }
            }
            for (int i = 0; i < trainees.size(); i++) {
                TraineeData traineeData = trainees.get(i);
                try {
                    Trainee trainee = parseTrainee(traineeData);
                    putIfAbsent(traineeMap, traineeData.id(), trainee, "trainee");
                    traineesLoaded++;
                } catch (RuntimeException exception) {
                    throw invalidEntry("trainees", i, exception);
                }
            }
            for (int i = 0; i < trainings.size(); i++) {
                TrainingData trainingData = trainings.get(i);
                try {
                    Training training = parseTraining(trainingData);
                    putIfAbsent(trainingMap, trainingData.id(), training, "training");
                    trainingsLoaded++;
                } catch (RuntimeException exception) {
                    throw invalidEntry("trainings", i, exception);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read initialization file: " + initFilePath, exception);
        }

        trainerIdSequence.set(maxId(trainerMap));
        traineeIdSequence.set(maxId(traineeMap));
        trainingIdSequence.set(maxId(trainingMap));

        log.info("Loaded initial data: {} trainers, {} trainees, {} trainings",
                trainersLoaded, traineesLoaded, trainingsLoaded);
    }

    public Trainer insertTrainer(Trainer trainer) {
        Objects.requireNonNull(trainer, "trainer must not be null");
        return insertWithGeneratedId(trainerMap, trainerIdSequence, id -> new Trainer(
                trainer.getFirstName(), trainer.getLastName(), trainer.getUsername(), trainer.getPassword(),
                trainer.isActive(), trainer.getTrainingTypes(), id));
    }

    public Trainee insertTrainee(Trainee trainee) {
        Objects.requireNonNull(trainee, "trainee must not be null");
        return insertWithGeneratedId(traineeMap, traineeIdSequence, id -> new Trainee(
                trainee.getFirstName(), trainee.getLastName(), trainee.getUsername(), trainee.getPassword(),
                trainee.isActive(), trainee.getDateOfBirth(), trainee.getAddress(), id));
    }

    public Training insertTraining(Training training) {
        Objects.requireNonNull(training, "training must not be null");
        return insertWithGeneratedId(trainingMap, trainingIdSequence, id -> new Training(
                id, training.getTraineeId(), training.getTrainerId(), training.getTrainingName(),
                training.getTrainingType(), training.getTrainingDateTime(), training.getTrainingDuration()));
    }

    private <T> T insertWithGeneratedId(Map<Long, T> map, AtomicLong sequence, LongFunction<T> factory) {
        while (true) {
            long highestStoredId = maxId(map);
            long id = sequence.updateAndGet(previousId -> {
                long highestId = Math.max(previousId, highestStoredId);
                if (highestId == Long.MAX_VALUE) {
                    throw new IllegalStateException("Cannot generate an ID: maximum value reached");
                }
                return highestId + 1;
            });
            T entity = factory.apply(id);
            if (map.putIfAbsent(id, entity) == null) {
                return entity;
            }
        }
    }

    private long maxId(Map<Long, ?> map) {
        return Math.max(0L, map.keySet().stream().mapToLong(Long::longValue).max().orElse(0L));
    }

    private Trainer parseTrainer(TrainerData data) {
        long id = requireId(data.id(), "trainer id");
        long userId = requireId(data.userId(), "trainer userId");
        requireProfileUserId(id, userId);
        return new Trainer(
                data.firstName(),
                data.lastName(),
                data.username(),
                data.password(),
                requireActive(data.isActive()),
                Set.copyOf(requireTrainingTypes(data.trainingTypes())),
                userId);
    }

    private Trainee parseTrainee(TraineeData data) {
        long id = requireId(data.id(), "trainee id");
        long userId = requireId(data.userId(), "trainee userId");
        requireProfileUserId(id, userId);
        return new Trainee(
                data.firstName(),
                data.lastName(),
                data.username(),
                data.password(),
                requireActive(data.isActive()),
                LocalDate.parse(data.dateOfBirth()),
                data.address(),
                userId);
    }

    private void requireProfileUserId(long id, long userId) {
        if (id != userId) {
            throw new IllegalArgumentException("Profile id " + id + " does not match userId " + userId);
        }
    }

    private Training parseTraining(TrainingData data) {
        return new Training(
                requireId(data.id(), "training id"),
                requireId(data.traineeId(), "training traineeId"),
                requireId(data.trainerId(), "training trainerId"),
                data.trainingName(),
                data.trainingType(),
                LocalDateTime.parse(data.trainingDateTime()),
                requireId(data.trainingDuration(), "training duration"));
    }

    private List<TrainingType> requireTrainingTypes(List<TrainingType> trainingTypes) {
        if (trainingTypes == null || trainingTypes.isEmpty()) {
            throw new IllegalArgumentException("Trainer must have at least one training type");
        }
        return trainingTypes;
    }

    private boolean requireActive(Boolean active) {
        if (active == null) {
            throw new IllegalArgumentException("isActive must be specified");
        }
        return active;
    }

    private long requireId(Long id, String fieldName) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " must be specified");
        }
        return id;
    }

    private <T> List<T> requireEntries(List<T> entries, String collection) {
        if (entries == null) {
            throw new IllegalArgumentException(
                    "Initialization data must contain the " + collection + " array");
        }
        return entries;
    }

    private IllegalArgumentException invalidEntry(String collection, int index, RuntimeException cause) {
        return new IllegalArgumentException(
                "Invalid initialization data at " + collection + "[" + index + "]", cause);
    }

    private <T> void putIfAbsent(Map<Long, T> map, long id, T value, String namespace) {
        if (map.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("Duplicate " + namespace + " id: " + id);
        }
    }

    private record InitializationData(
            List<TrainerData> trainers,
            List<TraineeData> trainees,
            List<TrainingData> trainings) {
    }

    private record TrainerData(
            Long id,
            String firstName,
            String lastName,
            String username,
            String password,
            Boolean isActive,
            List<TrainingType> trainingTypes,
            Long userId) {
    }

    private record TraineeData(
            Long id,
            String firstName,
            String lastName,
            String username,
            String password,
            Boolean isActive,
            String dateOfBirth,
            String address,
            Long userId) {
    }

    private record TrainingData(
            Long id,
            Long traineeId,
            Long trainerId,
            String trainingName,
            TrainingType trainingType,
            String trainingDateTime,
            Long trainingDuration) {
    }

}