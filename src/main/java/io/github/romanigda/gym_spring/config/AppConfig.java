package io.github.romanigda.gym_spring.config;

import io.github.romanigda.gym_spring.GymSpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackageClasses = GymSpringApplication.class)
@Import(StorageConfig.class)
public class AppConfig {
}
