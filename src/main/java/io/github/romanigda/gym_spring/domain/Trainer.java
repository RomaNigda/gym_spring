package io.github.romanigda.gym_spring.domain;

import lombok.Getter;

import java.util.Set;

@Getter
public class Trainer extends User {
    private Set<TrainingType> trainingTypes;
    private Long userId;

    public Trainer() {
    }

    public Trainer(String firstName, String lastName, boolean isActive, Set<TrainingType> trainingTypes) {
        this(firstName, lastName, null, null, isActive, trainingTypes, null);
    }

    public Trainer(String firstName, String lastName, String username, String password,
                   boolean isActive, Set<TrainingType> trainingTypes, Long userId) {
        super(firstName, lastName, username, password, isActive);
        this.trainingTypes = trainingTypes;
        this.userId = userId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Trainer{");
        sb.append("trainingTypes=").append(trainingTypes);
        sb.append(", userId=").append(userId);
        sb.append(", firstName='").append(firstName).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", isActive=").append(isActive);
        sb.append('}');
        return sb.toString();
    }
}
