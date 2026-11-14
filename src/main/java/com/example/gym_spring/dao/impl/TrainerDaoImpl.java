package com.example.gym_spring.dao.impl;

import com.example.gym_spring.dao.TrainerDao;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.storage.Storage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TrainerDaoImpl implements TrainerDao {
    private final Storage storage;

    public TrainerDaoImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void save(Trainer trainer) {
        Long id = trainer.getUserId();
        if (storage.getTrainerMap().putIfAbsent(id, trainer) != null) {
            throw new IllegalArgumentException("Trainer already exists: " + id);
        }
    }

    @Override
    public void update(Trainer trainer) {
        Long id = trainer.getUserId();
        if (storage.getTrainerMap().replace(id, trainer) == null) {
            throw new IllegalArgumentException("Trainer not found: " + id);
        }
    }

    @Override
    public void delete(Long id) {
        if (storage.getTrainerMap().remove(id) == null) {
            throw new IllegalArgumentException("Trainer not found: " + id);
        }
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
