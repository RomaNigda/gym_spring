package com.example.gym_spring;

import com.example.gym_spring.config.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class GymSpringApplication {

    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            System.out.println("Spring context loaded with " + context.getBeanDefinitionCount() + " beans.");
        }
    }

}
