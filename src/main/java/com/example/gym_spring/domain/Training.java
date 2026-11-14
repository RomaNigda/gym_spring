package com.example.gym_spring.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Training {
    private Long traineeId;
    private Long trainerId;
    private String trainingName;
    private TrainingType trainingType;
    private LocalDateTime trainingDateTime;
    private Long trainingDuration;

    public Training() {
    }

    public Training(Long traineeId, Long trainerId, String trainingName, TrainingType trainingType,
                    LocalDateTime trainingDateTime, Long trainingDuration) {
        this.traineeId = traineeId;
        this.trainerId = trainerId;
        this.trainingName = trainingName;
        this.trainingType = trainingType;
        this.trainingDateTime = trainingDateTime;
        this.trainingDuration = trainingDuration;
    }
}
