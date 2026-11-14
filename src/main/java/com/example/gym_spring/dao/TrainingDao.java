package com.example.gym_spring.dao;

import com.example.gym_spring.domain.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingDao {
    void save(Training training);

    void update(Training training);

    void delete(Long id);

    Optional<Training> findById(Long id);

    List<Training> findAll();
}
