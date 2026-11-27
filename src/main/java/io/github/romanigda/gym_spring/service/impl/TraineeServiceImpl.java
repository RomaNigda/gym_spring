package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.exception.EntityNotFoundException;
import io.github.romanigda.gym_spring.exception.ValidationException;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.util.ServiceValidation;
import io.github.romanigda.gym_spring.util.UserCredentialsGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TraineeServiceImpl implements TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeServiceImpl.class);

    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final UserCredentialsGenerator credentialsGenerator;

    public TraineeServiceImpl(TraineeDao traineeDao, TrainerDao trainerDao,
                              UserCredentialsGenerator credentialsGenerator) {
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
        this.credentialsGenerator = credentialsGenerator;
    }

    @Override
    public Trainee create(Trainee trainee) {
        validateTrainee(trainee);
        Trainee profile = new Trainee(trainee.getFirstName().trim(), trainee.getLastName().trim(), true,
                trainee.getDateOfBirth(), trainee.getAddress());
        credentialsGenerator.generateCredentials(profile);
        Trainee created = traineeDao.save(profile);
        log.info("Created trainee profile with id {}", created.getId());
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Trainee selectByUsername(String username) {
        Trainee trainee = findExisting(username);
        log.debug("Selected trainee by username");
        return trainee;
    }

    @Override
    public void changePassword(String username, String newPassword) {
        String password = ServiceValidation.requireText(newPassword, "newPassword");
        findExisting(username).setPassword(password);
        log.info("Changed password for trainee {}", username);
    }

    @Override
    public Trainee update(Trainee trainee) {
        validateTrainee(trainee);
        Trainee existing = findExisting(trainee.getUsername());
        existing.setFirstName(trainee.getFirstName().trim());
        existing.setLastName(trainee.getLastName().trim());
        existing.setDateOfBirth(trainee.getDateOfBirth());
        existing.setAddress(trainee.getAddress());
        Trainee updated = traineeDao.update(existing);
        log.info("Updated trainee profile with id {}", updated.getId());
        return updated;
    }

    @Override
    public boolean toggleActive(String username) {
        Trainee trainee = findExisting(username);
        trainee.setActive(!Boolean.TRUE.equals(trainee.getActive()));
        log.info("Trainee {} active state changed to {}", username, trainee.getActive());
        return trainee.getActive();
    }

    @Override
    public void deleteByUsername(String username) {
        Trainee trainee = findExisting(username);
        traineeDao.delete(trainee);
        log.info("Deleted trainee profile with id {}", trainee.getId());
    }

    @Override
    public Trainee updateTrainers(String username, List<String> trainerUsernames) {
        if (trainerUsernames == null) {
            throw new ValidationException("trainerUsernames must not be null");
        }
        Trainee trainee = findExisting(username);
        Set<String> requested = new LinkedHashSet<>();
        for (String trainerUsername : trainerUsernames) {
            requested.add(ServiceValidation.requireText(trainerUsername, "trainer username"));
        }
        List<Trainer> trainers = trainerDao.findByUsernames(requested);
        if (trainers.size() != requested.size()) {
            Set<String> found = trainers.stream()
                    .map(Trainer::getUsername)
                    .collect(Collectors.toSet());
            requested.removeAll(found);
            log.warn("Cannot update trainers of trainee {}: unknown trainers {}", username, requested);
            throw new EntityNotFoundException("Trainers not found: " + requested);
        }
        trainee.setTrainers(new HashSet<>(trainers));
        log.info("Updated trainers list of trainee {} ({} trainers)", username, trainers.size());
        return trainee;
    }

    private Trainee findExisting(String username) {
        String validatedUsername = ServiceValidation.requireText(username, "username");
        return traineeDao.findByUsername(validatedUsername).orElseThrow(() -> {
            log.warn("Trainee {} was not found", validatedUsername);
            return new EntityNotFoundException("Trainee not found for username: " + validatedUsername);
        });
    }

    private void validateTrainee(Trainee trainee) {
        if (trainee == null) {
            throw new ValidationException("Trainee and its user data must not be null");
        }
        List<String> errors = new ArrayList<>();
        ServiceValidation.collectTextError(trainee.getFirstName(), "firstName", errors);
        ServiceValidation.collectTextError(trainee.getLastName(), "lastName", errors);
        if (trainee.getDateOfBirth() != null && trainee.getDateOfBirth().isAfter(LocalDate.now())) {
            errors.add("dateOfBirth must not be in the future");
        }
        ServiceValidation.throwIfErrors(errors);
    }
}
