package com.example.gym_spring.service.impl;

import com.example.gym_spring.dao.TraineeDao;
import com.example.gym_spring.dao.TrainerDao;
import com.example.gym_spring.domain.Trainee;
import com.example.gym_spring.exception.EntityNotFoundException;
import com.example.gym_spring.exception.ValidationException;
import com.example.gym_spring.service.TraineeService;
import com.example.gym_spring.util.PasswordGenerator;
import com.example.gym_spring.util.ServiceValidation;
import com.example.gym_spring.util.UsernameGenerator;
import com.example.gym_spring.util.UsernameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TraineeServiceImpl implements TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeServiceImpl.class);

    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;

    public TraineeServiceImpl(TraineeDao traineeDao, TrainerDao trainerDao) {
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
    }

    @Override
    public Trainee create(Trainee trainee) {
        validateTrainee(trainee, false);

        String firstName = ServiceValidation.requireText(trainee.getFirstName(), "firstName");
        String lastName = ServiceValidation.requireText(trainee.getLastName(), "lastName");
        String username = UsernameGenerator.generateUsername(
                firstName, lastName, UsernameUtils.existingUsernames(traineeDao, trainerDao));
        Trainee draft = new Trainee(
                firstName,
                lastName,
                username,
                PasswordGenerator.generatePassword(),
                trainee.isActive(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                null);
        Trainee created = traineeDao.save(draft);
        log.info("Created trainee with id {}", created.getUserId());
        return created;
    }

    @Override
    public Trainee update(Trainee trainee) {
        validateTrainee(trainee, true);
        Long id = trainee.getUserId();
        if (traineeDao.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Trainee not found: " + id);
        }
        Trainee updated = new Trainee(
                ServiceValidation.requireText(trainee.getFirstName(), "firstName"),
                ServiceValidation.requireText(trainee.getLastName(), "lastName"),
                ServiceValidation.requireText(trainee.getUsername(), "username"),
                ServiceValidation.requireText(trainee.getPassword(), "password"),
                trainee.isActive(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                id);
        UsernameUtils.requireUniqueUsername(
                updated.getUsername(), traineeDao, trainerDao, id, null);
        try {
            traineeDao.update(updated);
        } catch (IllegalArgumentException exception) {
            throw new EntityNotFoundException("Trainee not found: " + id, exception);
        }
        log.info("Updated trainee with id {}", id);
        return updated;
    }

    @Override
    public void delete(Long id) {
        ServiceValidation.requirePositiveId(id, "trainee id");
        if (traineeDao.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Trainee not found: " + id);
        }
        try {
            traineeDao.delete(id);
        } catch (IllegalArgumentException exception) {
            throw new EntityNotFoundException("Trainee not found: " + id, exception);
        }
        log.info("Deleted trainee with id {}", id);
    }

    @Override
    public Trainee selectById(Long id) {
        ServiceValidation.requirePositiveId(id, "trainee id");
        Trainee trainee = traineeDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trainee not found: " + id));
        log.info("Selected trainee with id {}", id);
        return trainee;
    }

    @Override
    public Trainee selectByUsername(String username) {
        String validatedUsername = ServiceValidation.requireText(username, "username");
        Trainee trainee = traineeDao.findByUsername(validatedUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainee not found for username: " + validatedUsername));
        log.info("Selected trainee by username");
        return trainee;
    }

    @Override
    public List<Trainee> selectAll() {
        log.info("Selected all trainees");
        return traineeDao.findAll();
    }

    private void validateTrainee(Trainee trainee, boolean isUpdate) {
        if (trainee == null) {
            throw new ValidationException("Trainee must not be null");
        }
        ServiceValidation.requireText(trainee.getFirstName(), "firstName");
        ServiceValidation.requireText(trainee.getLastName(), "lastName");
        if (trainee.getDateOfBirth() == null) {
            throw new ValidationException("dateOfBirth must not be null");
        }
        if (trainee.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ValidationException("dateOfBirth must not be in the future");
        }
        if (isUpdate) {
            ServiceValidation.requirePositiveId(trainee.getUserId(), "trainee id");
            ServiceValidation.requireText(trainee.getUsername(), "username");
        }
    }
}
