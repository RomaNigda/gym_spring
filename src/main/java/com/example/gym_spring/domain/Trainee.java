package com.example.gym_spring.domain;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class Trainee extends User {
    private LocalDate dateOfBirth;
    private String address;
    private Long userId;

    public Trainee() {
    }

    public Trainee(String firstName, String lastName, String username, String password,
                   boolean isActive, LocalDate dateOfBirth, String address, Long userId) {
        super(firstName, lastName, username, password, isActive);
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.userId = userId;
    }
}
