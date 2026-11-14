package com.example.gym_spring.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class User {
    protected String firstName;
    protected String lastName;
    protected String username;
    protected String password;
    protected boolean isActive;
}
