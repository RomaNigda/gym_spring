package com.example.gym_spring.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Trainee extends User {
    private LocalDate dateOfBirth;
    private String address;
    private Long userId;
}
