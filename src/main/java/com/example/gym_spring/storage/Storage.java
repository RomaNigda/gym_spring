package com.example.gym_spring.storage;

import com.example.gym_spring.domain.Trainee;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.domain.Training;
import com.example.gym_spring.domain.TrainingType;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Component
public class Storage {

    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    // ===== 5. Геттери для DAO =====
    // ===== 1. Посилання на біни-мапи (namespace = ім’я біна) =====
    @Getter
    private final Map<Long, Trainer> trainerMap;
    @Getter
    private final Map<Long, Trainee> traineeMap;
    @Getter
    private final Map<Long, Training> trainingMap;
    @Getter
    private final Map<Long, TrainingType> trainingTypeMap;

    // ===== 2. Шлях до файлу з property placeholder =====
    @Value("${storage.init.file}")
    private String initFilePath;

    private final ResourceLoader resourceLoader;

    // ===== 3. Конструктор: інжектимо чотири мапи =====
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

    // ===== 4. Ініціалізація з файлу після створення біна =====
    @PostConstruct
    public void init() {
        log.info("Loading initial data from {}", initFilePath);
        Resource resource = resourceLoader.getResource(initFilePath);
        int trainersLoaded = 0;
        int traineesLoaded = 0;
        int trainingsLoaded = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    continue;
                }

                String[] fields = trimmedLine.split(";", -1);
                try {
                    switch (fields[0].trim()) {
                        case "trainer" -> {
                            Trainer trainer = parseTrainer(fields);
                            putIfAbsent(trainerMap, parseId(fields[1]), trainer, "trainer");
                            trainersLoaded++;
                        }
                        case "trainee" -> {
                            Trainee trainee = parseTrainee(fields);
                            putIfAbsent(traineeMap, parseId(fields[1]), trainee, "trainee");
                            traineesLoaded++;
                        }
                        case "training" -> {
                            Training training = parseTraining(fields);
                            putIfAbsent(trainingMap, parseId(fields[1]), training, "training");
                            trainingsLoaded++;
                        }
                        default -> throw new IllegalArgumentException(
                                "Unknown namespace: " + fields[0].trim());
                    }
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException(
                            "Invalid initialization data at line " + lineNumber + ": " + trimmedLine,
                            exception);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read initialization file: " + initFilePath, exception);
        }

        log.info("Loaded initial data: {} trainers, {} trainees, {} trainings",
                trainersLoaded, traineesLoaded, trainingsLoaded);
    }

    private Trainer parseTrainer(String[] fields) {
        requireFieldCount(fields, 9, "trainer");
        return new Trainer(
                fields[2].trim(),
                fields[3].trim(),
                fields[4].trim(),
                fields[5].trim(),
                parseBoolean(fields[6]),
                Set.of(parseTrainingType(fields[7])),
                parseId(fields[8]));
    }

    private Trainee parseTrainee(String[] fields) {
        requireFieldCount(fields, 10, "trainee");
        return new Trainee(
                fields[2].trim(),
                fields[3].trim(),
                fields[4].trim(),
                fields[5].trim(),
                parseBoolean(fields[6]),
                LocalDate.parse(fields[7].trim()),
                fields[8].trim(),
                parseId(fields[9]));
    }

    private Training parseTraining(String[] fields) {
        requireFieldCount(fields, 8, "training");
        return new Training(
                parseId(fields[1]),
                parseId(fields[2]),
                parseId(fields[3]),
                fields[4].trim(),
                parseTrainingType(fields[5]),
                LocalDateTime.parse(fields[6].trim()),
                parseId(fields[7]));
    }

    private TrainingType parseTrainingType(String field) {
        return TrainingType.valueOf(field.trim());
    }

    private long parseId(String field) {
        return Long.parseLong(field.trim());
    }

    private boolean parseBoolean(String field) {
        String value = field.trim();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Expected true or false, got: " + value);
        }
        return Boolean.parseBoolean(value);
    }

    private void requireFieldCount(String[] fields, int expectedCount, String namespace) {
        if (fields.length != expectedCount) {
            throw new IllegalArgumentException(
                    "Expected " + expectedCount + " fields for " + namespace + ", got " + fields.length);
        }
    }

    private <T> void putIfAbsent(Map<Long, T> map, long id, T value, String namespace) {
        if (map.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("Duplicate " + namespace + " id: " + id);
        }
    }

}