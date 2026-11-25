package io.github.romanigda.gym_spring.service;

import io.github.romanigda.gym_spring.domain.Trainee;

import java.util.List;

public interface TraineeService {
    /** Creates an active trainee with generated username and password from the user's first/last name. */
    Trainee create(Trainee trainee);

    Trainee selectByUsername(String username);

    void changePassword(String username, String newPassword);

    /** Updates first name, last name, date of birth and address of the trainee identified by username. */
    Trainee update(Trainee trainee);

    /** Switches the active flag to the opposite value and returns the new state. */
    boolean toggleActive(String username);

    /** Hard-deletes the trainee; related trainings are removed by cascade. */
    void deleteByUsername(String username);

    /** Replaces the trainee's trainers with the trainers identified by the given usernames. */
    Trainee updateTrainers(String username, List<String> trainerUsernames);
}
