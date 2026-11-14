package com.example.gym_spring.dao.impl;

import com.example.gym_spring.dao.TrainingDao;
import com.example.gym_spring.domain.Training;
import com.example.gym_spring.storage.Storage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingDaoImpl implements TrainingDao {
    private final Storage storage;

    public TrainingDaoImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Training save(Training training) {
        return storage.insertTraining(training);
    }

    @Override
    public void update(Training training) {
        Long id = training.getId();
        if (storage.getTrainingMap().replace(id, training) == null) {
            throw new IllegalArgumentException("Training not found: " + id);
        }
    }

    @Override
    public void delete(Long id) {
        if (storage.getTrainingMap().remove(id) == null) {
            throw new IllegalArgumentException("Training not found: " + id);
        }
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
