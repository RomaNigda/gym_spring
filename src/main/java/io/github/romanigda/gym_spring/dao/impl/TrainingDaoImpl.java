package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingDaoImpl implements TrainingDao {
    private static final Logger log = LoggerFactory.getLogger(TrainingDaoImpl.class);

    private final Storage storage;

    public TrainingDaoImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Training save(Training training) {
        Training saved = storage.insertTraining(training);
        log.debug("Persisted training with id {}", saved.getId());
        return saved;
    }

    @Override
    public void update(Training training) {
        Long id = training.getId();
        if (storage.getTrainingMap().replace(id, training) == null) {
            log.warn("Cannot persist training update: id {} was not found", id);
            throw new IllegalArgumentException("Training not found: " + id);
        }
        log.debug("Persisted training update with id {}", id);
    }

    @Override
    public void delete(Long id) {
        if (storage.getTrainingMap().remove(id) == null) {
            log.warn("Cannot delete training: id {} was not found", id);
            throw new IllegalArgumentException("Training not found: " + id);
        }
        log.debug("Deleted training with id {}", id);
    }

    @Override
    public Optional<Training> findById(Long id) {
        return Optional.ofNullable(storage.getTrainingMap().get(id));
    }

    @Override
    public List<Training> findAll() {
        return List.copyOf(storage.getTrainingMap().values());
    }
}
