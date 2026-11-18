package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.Trainer;

import java.util.List;
import java.util.Optional;

public interface TrainerDao {
    /** Saves a new trainer with a generated ID, ignoring the input ID. */
    Trainer save(Trainer trainer);

    void update(Trainer trainer);

    void delete(Long id);

    Optional<Trainer> findById(Long id);

    List<Trainer> findAll();

    Optional<Trainer> findByUsername(String username);
}
