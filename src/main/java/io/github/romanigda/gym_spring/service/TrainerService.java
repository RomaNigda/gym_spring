package io.github.romanigda.gym_spring.service;

import io.github.romanigda.gym_spring.domain.Trainer;

import java.util.List;

public interface TrainerService {
    /** Creates an active trainer with generated username and password from the user's first/last name. */
    Trainer create(Trainer trainer);

    Trainer selectByUsername(String username);

    void changePassword(String username, String newPassword);

    /** Updates first name, last name and specialization of the trainer identified by username. */
    Trainer update(Trainer trainer);

    /** Switches the active flag to the opposite value and returns the new state. */
    boolean toggleActive(String username);

    List<Trainer> selectNotAssignedToTrainee(String traineeUsername);
}
