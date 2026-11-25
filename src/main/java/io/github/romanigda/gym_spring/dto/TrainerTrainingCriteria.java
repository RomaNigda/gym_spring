package io.github.romanigda.gym_spring.dto;

import java.time.LocalDate;

/** Optional filters for a trainer's trainings; {@code null} fields are ignored. */
public record TrainerTrainingCriteria(LocalDate fromDate, LocalDate toDate, String traineeName) {
    public static TrainerTrainingCriteria empty() {
        return new TrainerTrainingCriteria(null, null, null);
    }
}
