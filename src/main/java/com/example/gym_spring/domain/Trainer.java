package com.example.gym_spring.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Trainer extends User {
    private Set<TrainingType> trainingTypes;;
    private Long userId;
}
