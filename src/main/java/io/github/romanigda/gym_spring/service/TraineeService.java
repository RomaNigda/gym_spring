package io.github.romanigda.gym_spring.service;

import io.github.romanigda.gym_spring.domain.Trainee;

import java.util.List;

public interface TraineeService {
    /** Creates a trainee with generated credentials and ID; the input ID is ignored. */
    Trainee create(Trainee trainee);

    Trainee update(Trainee trainee);

    void delete(Long id);

    Trainee selectById(Long id);

    Trainee selectByUsername(String username);

    List<Trainee> selectAll();
}
