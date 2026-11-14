package com.example.gym_spring.dao;

import com.example.gym_spring.domain.Trainee;

import java.util.List;
import java.util.Optional;

public interface TraineeDao {
    /** Saves a new trainee with a generated ID, ignoring the input ID. */
    Trainee save(Trainee trainee);

    void update(Trainee trainee);

    void delete(Long id);

    Optional<Trainee> findById(Long id);

    List<Trainee> findAll();

    Optional<Trainee> findByUsername(String username);
}
