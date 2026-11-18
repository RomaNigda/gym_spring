package io.github.romanigda.gym_spring;

import io.github.romanigda.gym_spring.config.AppConfig;
import io.github.romanigda.gym_spring.facade.GymFacade;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Set;

public class GymSpringApplication {

    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            var facade = context.getBean(GymFacade.class);

            facade.selectAllTrainers().forEach(System.out::println);

            facade.createTrainer(new io.github.romanigda.gym_spring.domain.Trainer("John", "Doe", true, Set.of(io.github.romanigda.gym_spring.domain.TrainingType.CARDIO)));
            facade.createTrainer(new io.github.romanigda.gym_spring.domain.Trainer("John", "Doe", true, Set.of(io.github.romanigda.gym_spring.domain.TrainingType.CARDIO)));
            facade.createTrainer(new io.github.romanigda.gym_spring.domain.Trainer("John", "Doe", true, Set.of(io.github.romanigda.gym_spring.domain.TrainingType.CARDIO)));
            facade.createTrainer(new io.github.romanigda.gym_spring.domain.Trainer("John", "Doe", true, Set.of(io.github.romanigda.gym_spring.domain.TrainingType.CARDIO)));

            System.out.println("==========================");
            System.out.println("All trainers after creation:");
            facade.selectAllTrainers().forEach(System.out::println);


        }
    }

}
