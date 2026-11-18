package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.util.PasswordGenerator;
import io.github.romanigda.gym_spring.util.ServiceValidation;
import io.github.romanigda.gym_spring.util.UsernameGenerator;
import io.github.romanigda.gym_spring.util.UsernameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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
        log.debug("Created trainee account with id {}", created.getUserId());
        return created;
    }

    @Override
    public Trainee update(Trainee trainee) {
        validateTrainee(trainee, true);
        Long id = trainee.getUserId();
        if (traineeDao.findById(id).isEmpty()) {
            log.warn("Cannot update trainee: id {} was not found", id);
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
            log.warn("Cannot update trainee: id {} was not found", id);
            throw new EntityNotFoundException("Trainee not found: " + id, exception);
        }
        log.info("Updated trainee with id {}", id);
        return updated;
    }

    @Override
    public void delete(Long id) {
        ServiceValidation.requirePositiveId(id, "trainee id");
        if (traineeDao.findById(id).isEmpty()) {
            log.warn("Cannot delete trainee: id {} was not found", id);
            throw new EntityNotFoundException("Trainee not found: " + id);
        }
        try {
            traineeDao.delete(id);
        } catch (IllegalArgumentException exception) {
            log.warn("Cannot delete trainee: id {} was not found", id);
            throw new EntityNotFoundException("Trainee not found: " + id, exception);
        }
        log.info("Deleted trainee with id {}", id);
    }

    @Override
    public Trainee selectById(Long id) {
        ServiceValidation.requirePositiveId(id, "trainee id");
        Trainee trainee = traineeDao.findById(id).orElseThrow(() -> {
            log.warn("Trainee with id {} was not found", id);
            return new EntityNotFoundException("Trainee not found: " + id);
        });
        log.debug("Selected trainee with id {}", id);
        return trainee;
    }

    @Override
    public Trainee selectByUsername(String username) {
        String validatedUsername = ServiceValidation.requireText(username, "username");
        Trainee trainee = traineeDao.findByUsername(validatedUsername).orElseThrow(() -> {
            log.warn("Trainee was not found for the supplied username");
            return new EntityNotFoundException("Trainee not found for username: " + validatedUsername);
        });
        log.debug("Selected trainee by username");
        return trainee;
    }

    @Override
    public List<Trainee> selectAll() {
        log.debug("Selected all trainees");
        return traineeDao.findAll();
    }

    private void validateTrainee(Trainee trainee, boolean isUpdate) {
        if (trainee == null) {
            throw new ValidationException("Trainee must not be null");
        }
        List<String> errors = new ArrayList<>();
        ServiceValidation.collectTextError(trainee.getFirstName(), "firstName", errors);
        ServiceValidation.collectTextError(trainee.getLastName(), "lastName", errors);
        if (trainee.getDateOfBirth() == null) {
            errors.add("dateOfBirth must not be null");
        } else if (trainee.getDateOfBirth().isAfter(LocalDate.now())) {
            errors.add("dateOfBirth must not be in the future");
        }
        if (isUpdate) {
            ServiceValidation.collectPositiveIdError(trainee.getUserId(), "trainee id", errors);
            ServiceValidation.collectTextError(trainee.getUsername(), "username", errors);
            ServiceValidation.collectTextError(trainee.getPassword(), "password", errors);
        }
        ServiceValidation.throwIfErrors(errors);
    }
}
