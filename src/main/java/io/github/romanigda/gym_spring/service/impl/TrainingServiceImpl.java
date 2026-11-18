package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.TrainingService;
import io.github.romanigda.gym_spring.util.ServiceValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainingServiceImpl implements TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingServiceImpl.class);

    private final TrainingDao trainingDao;

    public TrainingServiceImpl(TrainingDao trainingDao) {
        this.trainingDao = trainingDao;
    }

    @Override
    public Training create(Training training) {
        validateTraining(training);
        Training created = trainingDao.save(training);
        log.info("Created training with id {}", created.getId());
        return created;
    }

    @Override
    public Training selectById(Long id) {
        ServiceValidation.requirePositiveId(id, "training id");
        Training training = trainingDao.findById(id).orElseThrow(() -> {
            log.warn("Training with id {} was not found", id);
            return new EntityNotFoundException("Training not found: " + id);
        });
        log.debug("Selected training with id {}", id);
        return training;
    }

    @Override
    public List<Training> selectAll() {
        log.debug("Selected all trainings");
        return trainingDao.findAll();
    }

    private void validateTraining(Training training) {
        if (training == null) {
            throw new ValidationException("Training must not be null");
        }
        ServiceValidation.requirePositiveId(training.getTraineeId(), "trainee id");
        ServiceValidation.requirePositiveId(training.getTrainerId(), "trainer id");
        ServiceValidation.requireText(training.getTrainingName(), "trainingName");
        if (training.getTrainingType() == null) {
            throw new ValidationException("trainingType must not be null");
        }
        if (training.getTrainingDateTime() == null) {
            throw new ValidationException("trainingDateTime must not be null");
        }
        if (training.getTrainingDuration() == null || training.getTrainingDuration() <= 0) {
            throw new ValidationException("trainingDuration must be a positive number");
        }
    }
}
