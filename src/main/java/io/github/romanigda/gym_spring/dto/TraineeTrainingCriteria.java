package io.github.romanigda.gym_spring.dto;

import java.time.LocalDate;

/** Optional filters for a trainee's trainings; {@code null} fields are ignored. */
public record TraineeTrainingCriteria(LocalDate fromDate, LocalDate toDate,
                                      String trainerName, String trainingTypeName) {
    public static TraineeTrainingCriteria empty() {
        return new TraineeTrainingCriteria(null, null, null, null);
    }
}
