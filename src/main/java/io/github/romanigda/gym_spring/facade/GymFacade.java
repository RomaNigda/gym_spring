package io.github.romanigda.gym_spring.facade;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.dto.NewTraining;
import io.github.romanigda.gym_spring.dto.TraineeTrainingCriteria;
import io.github.romanigda.gym_spring.dto.TrainerTrainingCriteria;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.AuthenticationService;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.service.TrainerService;
import io.github.romanigda.gym_spring.service.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Entry point for all gym operations; everything except profile creation requires valid credentials. */
@Component
public class GymFacade {
    private final AuthenticationService authenticationService;
    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(AuthenticationService authenticationService, TraineeService traineeService,
                     TrainerService trainerService, TrainingService trainingService) {
        this.authenticationService = authenticationService;
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    // 1. Create trainer profile
    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    // 2. Create trainee profile
    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    // 3. Trainee username and password matching
    public void authenticateTrainee(String username, String password) {
        authenticationService.authenticate(username, password);
        traineeService.selectByUsername(username);
    }

    // 4. Trainer username and password matching
    public void authenticateTrainer(String username, String password) {
        authenticationService.authenticate(username, password);
        trainerService.selectByUsername(username);
    }

    // 5. Select trainer profile by username
    public Trainer selectTrainer(String username, String password) {
        authenticationService.authenticate(username, password);
        return trainerService.selectByUsername(username);
    }

    // 6. Select trainee profile by username
    public Trainee selectTrainee(String username, String password) {
        authenticationService.authenticate(username, password);
        return traineeService.selectByUsername(username);
    }

    // 7. Trainee password change
    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        authenticationService.authenticate(username, oldPassword);
        traineeService.changePassword(username, newPassword);
    }

    // 8. Trainer password change
    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        authenticationService.authenticate(username, oldPassword);
        trainerService.changePassword(username, newPassword);
    }

    // 9. Update the authenticated trainer's profile
    public Trainer updateTrainer(String username, String password, Trainer trainer) {
        authenticateTrainer(username, password);
        if (trainer == null || trainer.getUser() == null) {
            throw new ValidationException("Trainer and its user data must not be null");
        }
        trainer.getUser().setUsername(username);
        return trainerService.update(trainer);
    }

    // 10. Update the authenticated trainee's profile
    public Trainee updateTrainee(String username, String password, Trainee trainee) {
        authenticateTrainee(username, password);
        if (trainee == null || trainee.getUser() == null) {
            throw new ValidationException("Trainee and its user data must not be null");
        }
        trainee.getUser().setUsername(username);
        return traineeService.update(trainee);
    }

    // 11. Activate/De-activate trainee (toggles the current state)
    public boolean toggleTraineeActive(String username, String password) {
        authenticateTrainer(username, password);
        return traineeService.toggleActive(username);
    }

    // 12. Activate/De-activate trainer (toggles the current state)
    public boolean toggleTrainerActive(String username, String password) {
        authenticateTrainer(username, password);
        return trainerService.toggleActive(username);
    }

    // 13. Delete trainee profile by username
    public void deleteTrainee(String username, String password) {
        authenticateTrainer(username, password);
        traineeService.deleteByUsername(username);
    }

    // 14. Get trainee trainings list by trainee username and criteria
    public List<Training> selectTraineeTrainings(String username, String password, TraineeTrainingCriteria criteria) {
        authenticationService.authenticate(username, password);
        return trainingService.selectTraineeTrainings(username, criteria);
    }

    // 15. Get trainer trainings list by trainer username and criteria
    public List<Training> selectTrainerTrainings(String username, String password, TrainerTrainingCriteria criteria) {
        authenticationService.authenticate(username, password);
        return trainingService.selectTrainerTrainings(username, criteria);
    }

    // 16. Add training
    public Training addTraining(String username, String password, NewTraining newTraining) {
        authenticateTrainer(username, password);
        return trainingService.addTraining(newTraining);
    }

    // 17. Get trainers list that are not assigned to the trainee
    public List<Trainer> selectTrainersNotAssignedToTrainee(String username, String password) {
        authenticationService.authenticate(username, password);
        return trainerService.selectNotAssignedToTrainee(username);
    }

    // 18. Update trainee's trainers list
    public Trainee updateTraineeTrainers(String username, String password, List<String> trainerUsernames) {
        authenticationService.authenticate(username, password);
        return traineeService.updateTrainers(username, trainerUsernames);
    }
}
