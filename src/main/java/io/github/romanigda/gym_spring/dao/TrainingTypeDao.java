package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.TrainingType;

import java.util.List;
import java.util.Optional;

public interface TrainingTypeDao {
    Optional<TrainingType> findById(Long id);

    Optional<TrainingType> findByName(String trainingTypeName);

    List<TrainingType> findAll();
}
