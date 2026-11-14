package com.example.gym_spring;

import com.example.gym_spring.config.AppConfig;
import com.example.gym_spring.dao.impl.TrainerDaoImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class GymSpringApplication {

    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            var bean = context.getBean(TrainerDaoImpl.class);
            System.out.println(bean.findAll());


        }
    }

}
