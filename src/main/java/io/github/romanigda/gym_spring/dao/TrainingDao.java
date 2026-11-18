package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingDao {
    /**
     * Persists the supplied training as a new record. Any ID already set on the training is ignored;
     * a new ID is generated.
     *
     * @param training the training data to persist
     * @return the persisted training with its generated ID
     */
    Training save(Training training);

    void update(Training training);

    void delete(Long id);

    Optional<Training> findById(Long id);

    List<Training> findAll();
}
