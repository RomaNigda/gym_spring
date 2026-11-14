package com.example.gym_spring;

import com.example.gym_spring.config.AppConfig;
import com.example.gym_spring.facade.GymFacade;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class GymSpringApplication {

    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            var facade = context.getBean(GymFacade.class);
            System.out.println(facade.selectAllTrainers());
        }
    }

}
