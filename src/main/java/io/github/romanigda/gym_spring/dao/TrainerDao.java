package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.Trainer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainerDao {
    Trainer save(Trainer trainer);

    Trainer update(Trainer trainer);

    /** Loads the trainer together with its user, specialization and assigned trainees. */
    Optional<Trainer> findByUsername(String username);

    List<Trainer> findByUsernames(Collection<String> usernames);

    List<Trainer> findTrainersNotAssignedToTrainee(String traineeUsername);
}
