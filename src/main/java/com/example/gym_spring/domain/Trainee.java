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

    public Trainee(String firstName, String lastName, boolean isActive, LocalDate dateOfBirth, String address) {
        this(firstName, lastName, null, null, isActive, dateOfBirth, address, null);
    }

    public Trainee(String firstName, String lastName, String username, String password,
                   boolean isActive, LocalDate dateOfBirth, String address, Long userId) {
        super(firstName, lastName, username, password, isActive);
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.userId = userId;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Trainee{");
        sb.append("dateOfBirth=").append(dateOfBirth);
        sb.append(", address='").append(address).append('\'');
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
