package com.example.gym_spring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(StorageConfig.class)
public class AppConfig {
}
