package com.example.gym_spring.domain;

import lombok.Getter;

@Getter
public class User {
    protected String firstName;
    protected String lastName;
    protected String username;
    protected String password;
    protected boolean isActive;

    public User() {
    }

    public User(String firstName, String lastName, String username, String password, boolean isActive) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.isActive = isActive;
    }
}
