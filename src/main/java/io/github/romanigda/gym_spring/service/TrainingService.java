package io.github.romanigda.gym_spring.service;

import io.github.romanigda.gym_spring.domain.Training;

import java.util.List;

public interface TrainingService {
    /** Creates a training with a generated ID; the input ID is ignored. */
    Training create(Training training);

    Training selectById(Long id);

    List<Training> selectAll();
}
