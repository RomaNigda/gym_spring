package io.github.romanigda.gym_spring.dto;

import java.time.LocalDate;

/**
 * Data required to add a training. {@code trainingTypeName} is optional and defaults to the
 * trainer's specialization.
 */
public record NewTraining(String traineeUsername, String trainerUsername, String trainingName,
                          String trainingTypeName, LocalDate trainingDate, Integer trainingDuration) {
}
