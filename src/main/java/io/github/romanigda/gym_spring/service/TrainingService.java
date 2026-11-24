package io.github.romanigda.gym_spring.service;

import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.dto.NewTraining;
import io.github.romanigda.gym_spring.dto.TraineeTrainingCriteria;
import io.github.romanigda.gym_spring.dto.TrainerTrainingCriteria;

import java.util.List;

public interface TrainingService {
    Training addTraining(NewTraining newTraining);

    List<Training> selectTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria);

    List<Training> selectTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria);
}
