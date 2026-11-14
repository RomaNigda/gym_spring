package com.example.gym_spring.service.impl;

import com.example.gym_spring.dao.TraineeDao;
import com.example.gym_spring.dao.TrainerDao;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.exception.EntityNotFoundException;
import com.example.gym_spring.exception.ValidationException;
import com.example.gym_spring.service.TrainerService;
import com.example.gym_spring.util.PasswordGenerator;
import com.example.gym_spring.util.ServiceValidation;
import com.example.gym_spring.util.UsernameGenerator;
import com.example.gym_spring.util.UsernameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainerServiceImpl implements TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerServiceImpl.class);

    private final TrainerDao trainerDao;
    private final TraineeDao traineeDao;

    public TrainerServiceImpl(TrainerDao trainerDao, TraineeDao traineeDao) {
        this.trainerDao = trainerDao;
        this.traineeDao = traineeDao;
    }

    @Override
    public Trainer create(Trainer trainer) {
        validateTrainer(trainer, false);

        String firstName = ServiceValidation.requireText(trainer.getFirstName(), "firstName");
        String lastName = ServiceValidation.requireText(trainer.getLastName(), "lastName");
        String username = UsernameGenerator.generateUsername(firstName, lastName, UsernameUtils.existingUsernames(traineeDao, trainerDao));
        Trainer draft = new Trainer(
                firstName,
                lastName,
                username,
                PasswordGenerator.generatePassword(),
                trainer.isActive(),
                trainer.getTrainingTypes(),
                null);
        Trainer created = trainerDao.save(draft);
        log.info("Created trainer with id {}", created.getUserId());
        return created;
    }

    @Override
    public Trainer update(Trainer trainer) {
        validateTrainer(trainer, true);
        Long id = trainer.getUserId();
        if (trainerDao.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Trainer not found: " + id);
        }
        Trainer updated = new Trainer(
                ServiceValidation.requireText(trainer.getFirstName(), "firstName"),
                ServiceValidation.requireText(trainer.getLastName(), "lastName"),
                ServiceValidation.requireText(trainer.getUsername(), "username"),
                ServiceValidation.requireText(trainer.getPassword(), "password"),
                trainer.isActive(),
                trainer.getTrainingTypes(),
                id);
        UsernameUtils.requireUniqueUsername(
                updated.getUsername(), traineeDao, trainerDao, null, id);
        try {
            trainerDao.update(updated);
        } catch (IllegalArgumentException exception) {
            throw new EntityNotFoundException("Trainer not found: " + id, exception);
        }
        log.info("Updated trainer with id {}", id);
        return updated;
    }

    @Override
    public Trainer selectById(Long id) {
        ServiceValidation.requirePositiveId(id, "trainer id");
        Trainer trainer = trainerDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found: " + id));
        log.info("Selected trainer with id {}", id);
        return trainer;
    }

    @Override
    public Trainer selectByUsername(String username) {
        String validatedUsername = ServiceValidation.requireText(username, "username");
        Trainer trainer = trainerDao.findByUsername(validatedUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainer not found for username: " + validatedUsername));
        log.info("Selected trainer by username");
        return trainer;
    }

    @Override
    public List<Trainer> selectAll() {
        log.info("Selected all trainers");
        return trainerDao.findAll();
    }

    private void validateTrainer(Trainer trainer, boolean isUpdate) {
        if (trainer == null) {
            throw new ValidationException("Trainer must not be null");
        }
        ServiceValidation.requireText(trainer.getFirstName(), "firstName");
        ServiceValidation.requireText(trainer.getLastName(), "lastName");
        if (trainer.getTrainingTypes() == null || trainer.getTrainingTypes().isEmpty()) {
            throw new ValidationException("trainingTypes must not be empty");
        }
        if (isUpdate) {
            ServiceValidation.requirePositiveId(trainer.getUserId(), "trainer id");
            ServiceValidation.requireText(trainer.getUsername(), "username");
        }
    }
}
