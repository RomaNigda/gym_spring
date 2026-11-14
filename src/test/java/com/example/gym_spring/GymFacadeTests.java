package com.example.gym_spring;

import com.example.gym_spring.domain.Trainee;
import com.example.gym_spring.domain.Trainer;
import com.example.gym_spring.domain.Training;
import com.example.gym_spring.exception.ValidationException;
import com.example.gym_spring.facade.GymFacade;
import com.example.gym_spring.service.TraineeService;
import com.example.gym_spring.service.TrainerService;
import com.example.gym_spring.service.TrainingService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GymFacadeTests {
    private static final Trainee TRAINEE_INPUT = new Trainee();
    private static final Trainer TRAINER_INPUT = new Trainer();
    private static final Training TRAINING_INPUT = new Training();
    private static final Trainee TRAINEE_RESULT = new Trainee();
    private static final Trainer TRAINER_RESULT = new Trainer();
    private static final Training TRAINING_RESULT = new Training();
    private static final List<Trainee> TRAINEES = List.of(TRAINEE_RESULT);
    private static final List<Trainer> TRAINERS = List.of(TRAINER_RESULT);
    private static final List<Training> TRAININGS = List.of(TRAINING_RESULT);
    private static final Long ID = 321L;
    private static final String USERNAME = " Unchanged.Username ";

    private final Calls calls = new Calls();
    private final GymFacade facade = new GymFacade(
            new TraineeStub(), new TrainerStub(), new TrainingStub());

    @ParameterizedTest
    @MethodSource("delegations")
    void delegatesExactlyOnceWithUnchangedArgumentsAndResult(Delegation delegation) {
        assertSame(delegation.result(), delegation.invoke().apply(facade));
        assertCall(delegation);
    }

    @ParameterizedTest
    @MethodSource("delegations")
    void propagatesTheOriginalServiceException(Delegation delegation) {
        calls.failure = new ValidationException("Service failure");

        assertSame(calls.failure, assertThrows(ValidationException.class,
                () -> delegation.invoke().apply(facade)));
        assertCall(delegation);
    }

    private void assertCall(Delegation delegation) {
        assertEquals(1, calls.count);
        assertEquals(delegation.operation(), calls.operation);
        assertSame(delegation.argument(), calls.argument);
    }

    private static Stream<Delegation> delegations() {
        return Stream.of(
                new Delegation("trainee.create", TRAINEE_INPUT, TRAINEE_RESULT,
                        facade -> facade.createTrainee(TRAINEE_INPUT)),
                new Delegation("trainee.update", TRAINEE_INPUT, TRAINEE_RESULT,
                        facade -> facade.updateTrainee(TRAINEE_INPUT)),
                new Delegation("trainee.delete", ID, null, facade -> {
                    facade.deleteTrainee(ID);
                    return null;
                }),
                new Delegation("trainee.selectById", ID, TRAINEE_RESULT,
                        facade -> facade.selectTraineeById(ID)),
                new Delegation("trainee.selectByUsername", USERNAME, TRAINEE_RESULT,
                        facade -> facade.selectTraineeByUsername(USERNAME)),
                new Delegation("trainee.selectAll", null, TRAINEES, GymFacade::selectAllTrainees),
                new Delegation("trainer.create", TRAINER_INPUT, TRAINER_RESULT,
                        facade -> facade.createTrainer(TRAINER_INPUT)),
                new Delegation("trainer.update", TRAINER_INPUT, TRAINER_RESULT,
                        facade -> facade.updateTrainer(TRAINER_INPUT)),
                new Delegation("trainer.selectById", ID, TRAINER_RESULT,
                        facade -> facade.selectTrainerById(ID)),
                new Delegation("trainer.selectByUsername", USERNAME, TRAINER_RESULT,
                        facade -> facade.selectTrainerByUsername(USERNAME)),
                new Delegation("trainer.selectAll", null, TRAINERS, GymFacade::selectAllTrainers),
                new Delegation("training.create", TRAINING_INPUT, TRAINING_RESULT,
                        facade -> facade.createTraining(TRAINING_INPUT)),
                new Delegation("training.selectById", ID, TRAINING_RESULT,
                        facade -> facade.selectTrainingById(ID)),
                new Delegation("training.selectAll", null, TRAININGS, GymFacade::selectAllTrainings));
    }

    private record Delegation(String operation, Object argument, Object result,
                              Function<GymFacade, Object> invoke) {
        @Override
        public String toString() {
            return operation;
        }
    }

    private static class Calls {
        private int count;
        private String operation;
        private Object argument;
        private ValidationException failure;

        private <T> T record(String operation, Object argument, T result) {
            count++;
            this.operation = operation;
            this.argument = argument;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private class TraineeStub implements TraineeService {
        @Override
        public Trainee create(Trainee trainee) {
            return calls.record("trainee.create", trainee, TRAINEE_RESULT);
        }

        @Override
        public Trainee update(Trainee trainee) {
            return calls.record("trainee.update", trainee, TRAINEE_RESULT);
        }

        @Override
        public void delete(Long id) {
            calls.record("trainee.delete", id, null);
        }

        @Override
        public Trainee selectById(Long id) {
            return calls.record("trainee.selectById", id, TRAINEE_RESULT);
        }

        @Override
        public Trainee selectByUsername(String username) {
            return calls.record("trainee.selectByUsername", username, TRAINEE_RESULT);
        }

        @Override
        public List<Trainee> selectAll() {
            return calls.record("trainee.selectAll", null, TRAINEES);
        }
    }

    private class TrainerStub implements TrainerService {
        @Override
        public Trainer create(Trainer trainer) {
            return calls.record("trainer.create", trainer, TRAINER_RESULT);
        }

        @Override
        public Trainer update(Trainer trainer) {
            return calls.record("trainer.update", trainer, TRAINER_RESULT);
        }

        @Override
        public Trainer selectById(Long id) {
            return calls.record("trainer.selectById", id, TRAINER_RESULT);
        }

        @Override
        public Trainer selectByUsername(String username) {
            return calls.record("trainer.selectByUsername", username, TRAINER_RESULT);
        }

        @Override
        public List<Trainer> selectAll() {
            return calls.record("trainer.selectAll", null, TRAINERS);
        }
    }

    private class TrainingStub implements TrainingService {
        @Override
        public Training create(Training training) {
            return calls.record("training.create", training, TRAINING_RESULT);
        }

        @Override
        public Training selectById(Long id) {
            return calls.record("training.selectById", id, TRAINING_RESULT);
        }

        @Override
        public List<Training> selectAll() {
            return calls.record("training.selectAll", null, TRAININGS);
        }
    }
}
