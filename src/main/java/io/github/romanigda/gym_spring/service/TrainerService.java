package io.github.romanigda.gym_spring.service;

import io.github.romanigda.gym_spring.domain.Trainer;

import java.util.List;

public interface TrainerService {
    /** Creates a trainer with generated credentials and ID; the input ID is ignored. */
    Trainer create(Trainer trainer);

    Trainer update(Trainer trainer);

    Trainer selectById(Long id);

    Trainer selectByUsername(String username);

    List<Trainer> selectAll();
}
