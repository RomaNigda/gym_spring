package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.Trainee;

import java.util.Optional;

public interface TraineeDao {
    Trainee save(Trainee trainee);

    Trainee update(Trainee trainee);

    void delete(Trainee trainee);

    /** Loads the trainee together with its user and assigned trainers. */
    Optional<Trainee> findByUsername(String username);
}
