package io.github.romanigda.gym_spring.facade;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.service.TrainerService;
import io.github.romanigda.gym_spring.service.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GymFacade {
    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(TraineeService traineeService, TrainerService trainerService,
                     TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    public Trainee updateTrainee(Trainee trainee) {
        return traineeService.update(trainee);
    }

    public void deleteTrainee(Long id) {
        traineeService.delete(id);
    }

    public Trainee selectTraineeById(Long id) {
        return traineeService.selectById(id);
    }

    public Trainee selectTraineeByUsername(String username) {
        return traineeService.selectByUsername(username);
    }

    public List<Trainee> selectAllTrainees() {
        return traineeService.selectAll();
    }

    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    public Trainer updateTrainer(Trainer trainer) {
        return trainerService.update(trainer);
    }

    public Trainer selectTrainerById(Long id) {
        return trainerService.selectById(id);
    }

    public Trainer selectTrainerByUsername(String username) {
        return trainerService.selectByUsername(username);
    }

    public List<Trainer> selectAllTrainers() {
        return trainerService.selectAll();
    }

    public Training createTraining(Training training) {
        return trainingService.create(training);
    }

    public Training selectTrainingById(Long id) {
        return trainingService.selectById(id);
    }

    public List<Training> selectAllTrainings() {
        return trainingService.selectAll();
    }
}
