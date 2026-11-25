package io.github.romanigda.gym_spring;

import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.dto.NewTraining;
import io.github.romanigda.gym_spring.dto.TraineeTrainingCriteria;
import io.github.romanigda.gym_spring.facade.GymFacade;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;

public class GymSpringApplication {

    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            var facade = context.getBean(GymFacade.class);

            Trainer trainer = facade.createTrainer(
                    new Trainer("John", "Doe", true, new TrainingType(null, "CARDIO")));
            Trainee trainee = facade.createTrainee(
                    new Trainee("Jane", "Smith", true, LocalDate.of(1995, 5, 20), "Kyiv"));

            String traineeUsername = trainee.getUsername();
            String traineePassword = trainee.getPassword();
            String trainerUsername = trainer.getUsername();
            String trainerPassword = trainer.getPassword();

            System.out.println("Created: " + trainer + "\nCreated: " + trainee);

            System.out.println("Not assigned trainers: "
                    + facade.selectTrainersNotAssignedToTrainee(traineeUsername, traineePassword).size());

            facade.addTraining(trainerUsername, trainerPassword,
                    new NewTraining(traineeUsername, trainerUsername,
                            "Morning run", null, LocalDate.now(), 45));

            facade.selectTraineeTrainings(traineeUsername, traineePassword, TraineeTrainingCriteria.empty())
                    .forEach(System.out::println);

            facade.updateTraineeTrainers(traineeUsername, traineePassword, List.of(trainerUsername));
            System.out.println("Trainee active after toggle: "
                    + facade.toggleTraineeActive(traineeUsername, traineePassword));

            facade.deleteTrainee(traineeUsername, traineePassword);
            System.out.println("Deleted trainee " + traineeUsername);
        }
    }
}