package com.example.gym_spring.domain;

import lombok.Getter;

import java.util.Set;

@Getter
public class Trainer extends User {
    private Set<TrainingType> trainingTypes;
    private Long userId;

    public Trainer() {
    }

    public Trainer(String firstName, String lastName, String username, String password,
                   boolean isActive, Set<TrainingType> trainingTypes, Long userId) {
        super(firstName, lastName, username, password, isActive);
        this.trainingTypes = trainingTypes;
        this.userId = userId;
    }
}
