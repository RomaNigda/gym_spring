package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.Trainer;

import java.util.List;
import java.util.Optional;

public interface TrainerDao {
    /**
     * Persists the supplied trainer as a new record. Any ID already set on the trainer is ignored;
     * a new ID is generated.
     *
     * @param trainer the trainer data to persist
     * @return the persisted trainer with its generated ID
     */
    Trainer save(Trainer trainer);

    void update(Trainer trainer);

    void delete(Long id);

    Optional<Trainer> findById(Long id);

    List<Trainer> findAll();

    Optional<Trainer> findByUsername(String username);
}
