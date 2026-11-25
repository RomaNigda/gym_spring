package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.dao.TrainingTypeDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.dto.NewTraining;
import io.github.romanigda.gym_spring.dto.TraineeTrainingCriteria;
import io.github.romanigda.gym_spring.dto.TrainerTrainingCriteria;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.TrainingService;
import io.github.romanigda.gym_spring.util.ServiceValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TrainingServiceImpl implements TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingServiceImpl.class);

    private final TrainingDao trainingDao;
    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final TrainingTypeDao trainingTypeDao;

    public TrainingServiceImpl(TrainingDao trainingDao, TraineeDao traineeDao, TrainerDao trainerDao,
                               TrainingTypeDao trainingTypeDao) {
        this.trainingDao = trainingDao;
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
        this.trainingTypeDao = trainingTypeDao;
    }

    @Override
    public Training addTraining(NewTraining newTraining) {
        validate(newTraining);
        Trainee trainee = traineeDao.findByUsername(newTraining.traineeUsername().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainee not found for username: " + newTraining.traineeUsername()));
        Trainer trainer = trainerDao.findByUsername(newTraining.trainerUsername().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainer not found for username: " + newTraining.trainerUsername()));
        TrainingType trainingType = resolveTrainingType(newTraining.trainingTypeName(), trainer);

        Training training = trainingDao.save(new Training(trainee, trainer, newTraining.trainingName().trim(),
                trainingType, newTraining.trainingDate(), newTraining.trainingDuration()));
        trainee.getTrainers().add(trainer);
        log.info("Added training with id {} for trainee {} and trainer {}",
                training.getId(), trainee.getUsername(), trainer.getUsername());
        return training;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> selectTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria) {
        String username = ServiceValidation.requireText(traineeUsername, "trainee username");
        TraineeTrainingCriteria filter = criteria == null ? TraineeTrainingCriteria.empty() : criteria;
        validateDateRange(filter.fromDate(), filter.toDate());
        List<Training> trainings = trainingDao.findTraineeTrainings(username, filter.fromDate(), filter.toDate(),
                filter.trainerName(), filter.trainingTypeName());
        log.debug("Found {} trainings for trainee {}", trainings.size(), username);
        return trainings;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> selectTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria) {
        String username = ServiceValidation.requireText(trainerUsername, "trainer username");
        TrainerTrainingCriteria filter = criteria == null ? TrainerTrainingCriteria.empty() : criteria;
        validateDateRange(filter.fromDate(), filter.toDate());
        List<Training> trainings = trainingDao.findTrainerTrainings(username, filter.fromDate(), filter.toDate(),
                filter.traineeName());
        log.debug("Found {} trainings for trainer {}", trainings.size(), username);
        return trainings;
    }

    private TrainingType resolveTrainingType(String trainingTypeName, Trainer trainer) {
        if (trainingTypeName == null || trainingTypeName.isBlank()) {
            return trainer.getSpecialization();
        }
        return trainingTypeDao.findByName(trainingTypeName)
                .orElseThrow(() -> new EntityNotFoundException("Training type not found: " + trainingTypeName));
    }

    private void validate(NewTraining training) {
        if (training == null) {
            throw new ValidationException("Training must not be null");
        }
        List<String> errors = new ArrayList<>();
        ServiceValidation.collectTextError(training.traineeUsername(), "trainee username", errors);
        ServiceValidation.collectTextError(training.trainerUsername(), "trainer username", errors);
        ServiceValidation.collectTextError(training.trainingName(), "trainingName", errors);
        if (training.trainingDate() == null) {
            errors.add("trainingDate must not be null");
        }
        if (training.trainingDuration() == null || training.trainingDuration() <= 0) {
            errors.add("trainingDuration must be a positive number");
        }
        ServiceValidation.throwIfErrors(errors);
    }

    private static void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("fromDate must not be after toDate");
        }
    }
}
