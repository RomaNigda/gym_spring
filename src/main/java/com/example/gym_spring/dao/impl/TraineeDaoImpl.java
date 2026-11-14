package com.example.gym_spring.dao.impl;

import com.example.gym_spring.dao.TraineeDao;
import com.example.gym_spring.domain.Trainee;
import com.example.gym_spring.storage.Storage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TraineeDaoImpl implements TraineeDao {
    private final Storage storage;

    public TraineeDaoImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Trainee save(Trainee trainee) {
        return storage.insertTrainee(trainee);
    }

    @Override
    public void update(Trainee trainee) {
        Long id = trainee.getUserId();
        if (storage.getTraineeMap().replace(id, trainee) == null) {
            throw new IllegalArgumentException("Trainee not found: " + id);
        }
    }

    @Override
    public void delete(Long id) {
        if (storage.getTraineeMap().remove(id) == null) {
            throw new IllegalArgumentException("Trainee not found: " + id);
        }
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
