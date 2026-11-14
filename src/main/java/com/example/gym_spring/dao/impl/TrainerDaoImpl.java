package com.example.gym_spring.dao.impl;

import com.example.gym_spring.dao.TrainerDao;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TrainerDaoImpl implements TrainerDao {
    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    private final Storage storage;

    public TrainerDaoImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Trainer save(Trainer trainer) {
        Trainer saved = storage.insertTrainer(trainer);
        log.debug("Persisted trainer with id {}", saved.getUserId());
        return saved;
    }

    @Override
    public void update(Trainer trainer) {
        Long id = trainer.getUserId();
        if (storage.getTrainerMap().replace(id, trainer) == null) {
            log.warn("Cannot persist trainer update: id {} was not found", id);
            throw new IllegalArgumentException("Trainer not found: " + id);
        }
        log.debug("Persisted trainer update with id {}", id);
    }

    @Override
    public void delete(Long id) {
        if (storage.getTrainerMap().remove(id) == null) {
            log.warn("Cannot delete trainer: id {} was not found", id);
            throw new IllegalArgumentException("Trainer not found: " + id);
        }
        log.debug("Deleted trainer with id {}", id);
    }

    @Override
    public Optional<Trainer> findById(Long id) {
        return Optional.ofNullable(storage.getTrainerMap().get(id));
    }

    @Override
    public List<Trainer> findAll() {
        return List.copyOf(storage.getTrainerMap().values());
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        return storage.getTrainerMap().values().stream()
                .filter(trainer -> Objects.equals(trainer.getUsername(), username))
                .findFirst();
    }
}
