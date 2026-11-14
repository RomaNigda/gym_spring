package com.example.gym_spring.dao;

import com.example.gym_spring.domain.Trainer;

import java.util.List;
import java.util.Optional;

public interface TrainerDao {
    void save(Trainer trainer);

    void update(Trainer trainer);

    void delete(Long id);

    Optional<Trainer> findById(Long id);

    List<Trainer> findAll();

    Optional<Trainer> findByUsername(String username);
}
