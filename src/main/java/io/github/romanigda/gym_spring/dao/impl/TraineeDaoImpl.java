package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TraineeDaoImpl implements TraineeDao {
    private static final Logger log = LoggerFactory.getLogger(TraineeDaoImpl.class);

    private final Storage storage;

    public TraineeDaoImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Trainee save(Trainee trainee) {
        Trainee saved = storage.insertTrainee(trainee);
        log.debug("Persisted trainee with id {}", saved.getUserId());
        return saved;
    }

    @Override
    public void update(Trainee trainee) {
        Long id = trainee.getUserId();
        if (storage.getTraineeMap().replace(id, trainee) == null) {
            log.warn("Cannot persist trainee update: id {} was not found", id);
            throw new IllegalArgumentException("Trainee not found: " + id);
        }
        log.debug("Persisted trainee update with id {}", id);
    }

    @Override
    public void delete(Long id) {
        if (storage.getTraineeMap().remove(id) == null) {
            log.warn("Cannot delete trainee: id {} was not found", id);
            throw new IllegalArgumentException("Trainee not found: " + id);
        }
        log.debug("Deleted trainee with id {}", id);
    }

    @Override
    public Optional<Trainee> findById(Long id) {
        return Optional.ofNullable(storage.getTraineeMap().get(id));
    }

    @Override
    public List<Trainee> findAll() {
        return List.copyOf(storage.getTraineeMap().values());
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        return storage.getTraineeMap().values().stream()
                .filter(trainee -> Objects.equals(trainee.getUsername(), username))
                .findFirst();
    }
}
