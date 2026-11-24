package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.Training;

import java.time.LocalDate;
import java.util.List;

public interface TrainingDao {
    Training save(Training training);

    /** All criteria except the username are optional; {@code null} means "do not filter". */
    List<Training> findTraineeTrainings(String traineeUsername, LocalDate fromDate, LocalDate toDate,
                                        String trainerName, String trainingTypeName);

    /** All criteria except the username are optional; {@code null} means "do not filter". */
    List<Training> findTrainerTrainings(String trainerUsername, LocalDate fromDate, LocalDate toDate,
                                        String traineeName);
}
