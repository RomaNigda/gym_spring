package io.github.romanigda.gym_spring.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Training {
    private Long id;
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
        this(null, traineeId, trainerId, trainingName, trainingType, trainingDateTime, trainingDuration);
    }

    public Training(Long id, Long traineeId, Long trainerId, String trainingName, TrainingType trainingType,
                    LocalDateTime trainingDateTime, Long trainingDuration) {
        this.id = id;
        this.traineeId = traineeId;
        this.trainerId = trainerId;
        this.trainingName = trainingName;
        this.trainingType = trainingType;
        this.trainingDateTime = trainingDateTime;
        this.trainingDuration = trainingDuration;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Training{");
        sb.append("id=").append(id);
        sb.append(", traineeId=").append(traineeId);
        sb.append(", trainerId=").append(trainerId);
        sb.append(", trainingName='").append(trainingName).append('\'');
        sb.append(", trainingType=").append(trainingType);
        sb.append(", trainingDateTime=").append(trainingDateTime);
        sb.append(", trainingDuration=").append(trainingDuration);
        sb.append('}');
        return sb.toString();
    }
}
