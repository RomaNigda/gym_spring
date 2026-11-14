package com.example.gym_spring.dao;

import com.example.gym_spring.domain.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingDao {
    /** Saves a new training with a generated ID, ignoring the input ID. */
    Training save(Training training);

    void update(Training training);

    void delete(Long id);

    Optional<Training> findById(Long id);

    List<Training> findAll();
}
