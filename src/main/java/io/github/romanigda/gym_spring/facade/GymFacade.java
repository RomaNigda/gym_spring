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


    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    public void authenticateTrainee(String username, String password) {
        authenticationService.authenticate(username, password);
        traineeService.selectByUsername(username);
    }

    public void authenticateTrainer(String username, String password) {
        authenticationService.authenticate(username, password);
        trainerService.selectByUsername(username);
    }

    public Trainer selectTrainer(String username, String password) {
        authenticationService.authenticate(username, password);
        return trainerService.selectByUsername(username);
    }

    public Trainee selectTrainee(String username, String password) {
        authenticationService.authenticate(username, password);
        return traineeService.selectByUsername(username);
    }

    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        authenticationService.authenticate(username, oldPassword);
        traineeService.changePassword(username, newPassword);
    }

    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        authenticationService.authenticate(username, oldPassword);
        trainerService.changePassword(username, newPassword);
    }

    public Trainer updateTrainer(String username, String password, Trainer trainer) {
        authenticateTrainer(username, password);
        if (trainer == null) {
            throw new ValidationException("Trainer and its user data must not be null");
        }
        trainer.setUsername(username);
        return trainerService.update(trainer);
    }

    public Trainee updateTrainee(String username, String password, Trainee trainee) {
        authenticateTrainee(username, password);
        if (trainee == null) {
            throw new ValidationException("Trainee and its user data must not be null");
        }
        trainee.setUsername(username);
        return traineeService.update(trainee);
    }

    public boolean toggleTraineeActive(String username, String password) {
        authenticateTrainee(username, password);
        return traineeService.toggleActive(username);
    }

    public boolean toggleTrainerActive(String username, String password) {
        authenticateTrainer(username, password);
        return trainerService.toggleActive(username);
    }

    public void deleteTrainee(String username, String password) {
        authenticateTrainee(username, password);
        traineeService.deleteByUsername(username);
    }

    public List<Training> selectTraineeTrainings(String username, String password, TraineeTrainingCriteria criteria) {
        authenticationService.authenticate(username, password);
        return trainingService.selectTraineeTrainings(username, criteria);
    }

    public List<Training> selectTrainerTrainings(String username, String password, TrainerTrainingCriteria criteria) {
        authenticationService.authenticate(username, password);
        return trainingService.selectTrainerTrainings(username, criteria);
    }

    public Training addTraining(String username, String password, NewTraining newTraining) {
        authenticateTrainer(username, password);
        return trainingService.addTraining(newTraining);
    }

    public List<Trainer> selectTrainersNotAssignedToTrainee(String username, String password) {
        authenticationService.authenticate(username, password);
        return trainerService.selectNotAssignedToTrainee(username);
    }

    public Trainee updateTraineeTrainers(String username, String password, List<String> trainerUsernames) {
        authenticationService.authenticate(username, password);
        return traineeService.updateTrainers(username, trainerUsernames);
    }
}
