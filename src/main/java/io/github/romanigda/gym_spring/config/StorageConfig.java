package io.github.romanigda.gym_spring.config;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.storage.Storage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class StorageConfig {

    @Bean
    public Map<Long, Trainer> trainerStorage() {
        return new ConcurrentHashMap<>();
    }
    @Bean
    public Map<Long, Trainee> traineeStorage() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<Long, Training> trainingStorage() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<Long, TrainingType> trainingTypeStorage() {
        Map<Long, TrainingType> map = new ConcurrentHashMap<>();

        long id = 1L;
        for (TrainingType type : TrainingType.values()) {
            map.put(id++, type);
        }

        return map;
    }
}
