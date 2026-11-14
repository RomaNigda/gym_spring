package com.example.gym_spring.util;

import com.example.gym_spring.dao.TraineeDao;
import com.example.gym_spring.dao.TrainerDao;
import com.example.gym_spring.domain.Trainee;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.exception.DuplicateEntityException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class UsernameUtils {
    private UsernameUtils() {
    }

    public static Set<String> existingUsernames(TraineeDao traineeDao, TrainerDao trainerDao) {
        Set<String> usernames = new HashSet<>();
        traineeDao.findAll().stream()
                .map(Trainee::getUsername)
                .filter(Objects::nonNull)
                .forEach(usernames::add);
        trainerDao.findAll().stream()
                .map(Trainer::getUsername)
                .filter(Objects::nonNull)
                .forEach(usernames::add);
        return usernames;
    }

    public static void requireUniqueUsername(
            String username,
            TraineeDao traineeDao,
            TrainerDao trainerDao,
            Long excludedTraineeId,
            Long excludedTrainerId) {
        boolean usedByTrainee = traineeDao.findAll().stream()
                .anyMatch(trainee -> Objects.equals(trainee.getUsername(), username)
                        && !Objects.equals(trainee.getUserId(), excludedTraineeId));
        boolean usedByTrainer = trainerDao.findAll().stream()
                .anyMatch(trainer -> Objects.equals(trainer.getUsername(), username)
                        && !Objects.equals(trainer.getUserId(), excludedTrainerId));
        if (usedByTrainee || usedByTrainer) {
            throw new DuplicateEntityException("Username already exists: " + username);
        }
    }
}
