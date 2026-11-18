package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.Trainee;

import java.util.List;
import java.util.Optional;

public interface TraineeDao {
    /**
     * Persists the supplied trainee as a new record. Any ID already set on the trainee is ignored;
     * a new ID is generated.
     *
     * @param trainee the trainee data to persist
     * @return the persisted trainee with its generated ID
     */
    Trainee save(Trainee trainee);

    void update(Trainee trainee);

    void delete(Long id);

    Optional<Trainee> findById(Long id);

    List<Trainee> findAll();

    Optional<Trainee> findByUsername(String username);
}
