package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.dao.TrainingTypeDao;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.domain.User;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.TrainerService;
import io.github.romanigda.gym_spring.util.ServiceValidation;
import io.github.romanigda.gym_spring.util.UserCredentialsGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TrainerServiceImpl implements TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerServiceImpl.class);

    private final TrainerDao trainerDao;
    private final TraineeDao traineeDao;
    private final TrainingTypeDao trainingTypeDao;
    private final UserCredentialsGenerator credentialsGenerator;

    public TrainerServiceImpl(TrainerDao trainerDao, TraineeDao traineeDao, TrainingTypeDao trainingTypeDao,
                              UserCredentialsGenerator credentialsGenerator) {
        this.trainerDao = trainerDao;
        this.traineeDao = traineeDao;
        this.trainingTypeDao = trainingTypeDao;
        this.credentialsGenerator = credentialsGenerator;
    }

    @Override
    public Trainer create(Trainer trainer) {
        validateTrainer(trainer);
        TrainingType specialization = resolveTrainingType(trainer.getSpecialization());
        User user = credentialsGenerator.createUser(
                trainer.getUser().getFirstName().trim(), trainer.getUser().getLastName().trim());
        Trainer created = trainerDao.save(new Trainer(user, specialization));
        log.info("Created trainer profile with id {}", created.getId());
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Trainer selectByUsername(String username) {
        Trainer trainer = findExisting(username);
        log.debug("Selected trainer by username");
        return trainer;
    }

    @Override
    public void changePassword(String username, String newPassword) {
        String password = ServiceValidation.requireText(newPassword, "newPassword");
        findExisting(username).getUser().setPassword(password);
        log.info("Changed password for trainer {}", username);
    }

    @Override
    public Trainer update(Trainer trainer) {
        validateTrainer(trainer);
        Trainer existing = findExisting(trainer.getUser().getUsername());
        existing.getUser().setFirstName(trainer.getUser().getFirstName().trim());
        existing.getUser().setLastName(trainer.getUser().getLastName().trim());
        existing.setSpecialization(resolveTrainingType(trainer.getSpecialization()));
        Trainer updated = trainerDao.update(existing);
        log.info("Updated trainer profile with id {}", updated.getId());
        return updated;
    }

    @Override
    public boolean toggleActive(String username) {
        User user = findExisting(username).getUser();
        user.setActive(!user.isActive());
        log.info("Trainer {} active state changed to {}", username, user.isActive());
        return user.isActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> selectNotAssignedToTrainee(String traineeUsername) {
        String username = ServiceValidation.requireText(traineeUsername, "trainee username");
        if (traineeDao.findByUsername(username).isEmpty()) {
            log.warn("Trainee {} was not found", username);
            throw new EntityNotFoundException("Trainee not found for username: " + username);
        }
        List<Trainer> trainers = trainerDao.findTrainersNotAssignedToTrainee(username);
        log.debug("Found {} trainers not assigned to trainee {}", trainers.size(), username);
        return trainers;
    }

    private Trainer findExisting(String username) {
        String validatedUsername = ServiceValidation.requireText(username, "username");
        return trainerDao.findByUsername(validatedUsername).orElseThrow(() -> {
            log.warn("Trainer {} was not found", validatedUsername);
            return new EntityNotFoundException("Trainer not found for username: " + validatedUsername);
        });
    }

    private TrainingType resolveTrainingType(TrainingType trainingType) {
        if (trainingType.getId() != null) {
            return trainingTypeDao.findById(trainingType.getId()).orElseThrow(() ->
                    new EntityNotFoundException("Training type not found: " + trainingType.getId()));
        }
        return trainingTypeDao.findByName(trainingType.getTrainingTypeName()).orElseThrow(() ->
                new EntityNotFoundException("Training type not found: " + trainingType.getTrainingTypeName()));
    }

    private void validateTrainer(Trainer trainer) {
        if (trainer == null || trainer.getUser() == null) {
            throw new ValidationException("Trainer and its user data must not be null");
        }
        List<String> errors = new ArrayList<>();
        ServiceValidation.collectTextError(trainer.getUser().getFirstName(), "firstName", errors);
        ServiceValidation.collectTextError(trainer.getUser().getLastName(), "lastName", errors);
        TrainingType specialization = trainer.getSpecialization();
        if (specialization == null || (specialization.getId() == null
                && (specialization.getTrainingTypeName() == null || specialization.getTrainingTypeName().isBlank()))) {
            errors.add("specialization must not be null");
        }
        ServiceValidation.throwIfErrors(errors);
    }
}
