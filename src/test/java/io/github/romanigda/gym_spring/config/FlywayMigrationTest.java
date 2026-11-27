package io.github.romanigda.gym_spring.config;

import io.github.romanigda.gym_spring.dao.UserDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.dto.NewTraining;
import io.github.romanigda.gym_spring.dto.TraineeTrainingCriteria;
import io.github.romanigda.gym_spring.dto.TrainerTrainingCriteria;
import io.github.romanigda.gym_spring.exception.AuthenticationException;
import io.github.romanigda.gym_spring.facade.GymFacade;
import io.github.romanigda.gym_spring.service.AuthenticationService;
import io.github.romanigda.gym_spring.service.TraineeService;
import io.github.romanigda.gym_spring.service.TrainerService;
import io.github.romanigda.gym_spring.service.TrainingService;
import jakarta.validation.ConstraintViolationException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "GYM_TEST_DB_URL", matches = ".+")
class FlywayMigrationTest {
    private static final List<String> DEFAULT_TYPES = List.of(
            "BALANCE", "CARDIO", "FLEXIBILITY", "STRENGTH", "YOGA", "ZUMBA");

    private final List<AnnotationConfigApplicationContext> contexts = new ArrayList<>();
    private DataSource adminDataSource;
    private DataSource schemaDataSource;
    private String schema;
    private String schemaUrl;
    private String username;
    private String password;
    private boolean schemaCreated;

    @BeforeEach
    void createIsolatedSchema() throws SQLException {
        String url = requiredEnvironmentVariable("GYM_TEST_DB_URL");
        username = requiredEnvironmentVariable("GYM_TEST_DB_USERNAME");
        password = requiredEnvironmentVariable("GYM_TEST_DB_PASSWORD");
        schema = "gym_flyway_test_" + UUID.randomUUID().toString().replace("-", "");
        adminDataSource = new DriverManagerDataSource(url, username, password);

        try (Connection connection = adminDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            schemaCreated = true;
        }

        schemaUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        schemaDataSource = new DriverManagerDataSource(schemaUrl, username, password);
        try (Connection connection = schemaDataSource.getConnection()) {
            if (!schema.equals(connection.getSchema())) {
                throw new IllegalStateException("Test connection is not using the isolated schema");
            }
        }
    }

    @AfterEach
    void dropIsolatedSchema() throws SQLException {
        try {
            contexts.forEach(AnnotationConfigApplicationContext::close);
        } finally {
            if (schemaCreated) {
                try (Connection connection = adminDataSource.getConnection();
                     Statement statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            }
        }
    }

    @Test
    void migratesFreshSchemaBeforeHibernateAndOnlyOnce() throws SQLException {
        // Arrange
        AnnotationConfigApplicationContext context = newApplicationContext();
        AtomicReference<List<String>> typesBeforeHibernate = new AtomicReference<>();
        captureTypesBeforeHibernate(context, typesBeforeHibernate);

        // Act
        context.refresh();
        Trainer trainer = context.getBean(TrainerService.class)
                .create(new Trainer("Test", "Trainer", true, new TrainingType(null, "CARDIO")));
        MigrateResult secondMigration = context.getBean(Flyway.class).migrate();

        // Assert
        assertEquals(DEFAULT_TYPES, typesBeforeHibernate.get());
        assertEquals(DEFAULT_TYPES, trainingTypeNames());
        assertEquals("CARDIO", trainer.getSpecialization().getTrainingTypeName());
        assertEquals(0, secondMigration.migrationsExecuted);
        assertEquals(1, queryLong("SELECT count(*) FROM flyway_schema_history"
                + " WHERE version = '1' AND type = 'SQL' AND success"));
    }

    @Test
    void baselinesExistingSchemaWithoutReplacingData() throws SQLException {
        // Arrange
        execute("CREATE TABLE training_type (id BIGSERIAL PRIMARY KEY,"
                + " training_type_name VARCHAR(50) NOT NULL UNIQUE)");
        execute("INSERT INTO training_type (training_type_name) VALUES ('CARDIO'), ('LEGACY')");
        execute("CREATE TABLE legacy_marker (training_type_id BIGINT NOT NULL REFERENCES training_type(id),"
                + " note VARCHAR(50) NOT NULL)");
        execute("INSERT INTO legacy_marker SELECT id, 'preserved' FROM training_type"
                + " WHERE training_type_name = 'CARDIO'");
        long originalCardioId = queryLong("SELECT id FROM training_type WHERE training_type_name = 'CARDIO'");
        AnnotationConfigApplicationContext firstStart = newApplicationContext();
        AnnotationConfigApplicationContext secondStart = newApplicationContext();

        // Act
        firstStart.refresh();
        firstStart.close();
        secondStart.refresh();
        MigrateResult repeatedMigration = secondStart.getBean(Flyway.class).migrate();

        // Assert
        assertEquals(List.of("BALANCE", "CARDIO", "FLEXIBILITY", "LEGACY", "STRENGTH", "YOGA", "ZUMBA"),
                trainingTypeNames());
        assertEquals(originalCardioId,
                queryLong("SELECT id FROM training_type WHERE training_type_name = 'CARDIO'"));
        assertEquals(1, queryLong("SELECT count(*) FROM legacy_marker m JOIN training_type t"
                + " ON t.id = m.training_type_id WHERE t.training_type_name = 'CARDIO' AND m.note = 'preserved'"));
        assertEquals(1, queryLong("SELECT count(*) FROM flyway_schema_history"
                + " WHERE version = '0' AND type = 'BASELINE' AND success"));
        assertEquals(1, queryLong("SELECT count(*) FROM flyway_schema_history"
                + " WHERE version = '1' AND type = 'SQL' AND success"));
        assertEquals(0, repeatedMigration.migrationsExecuted);
    }

    @Test
    void persistsAndQueriesJoinedProfilesOnFreshSchema() throws SQLException {
        // Arrange
        AnnotationConfigApplicationContext context = newApplicationContext();
        context.refresh();
        TrainerService trainerService = context.getBean(TrainerService.class);
        TraineeService traineeService = context.getBean(TraineeService.class);
        TrainingService trainingService = context.getBean(TrainingService.class);
        AuthenticationService authenticationService = context.getBean(AuthenticationService.class);
        GymFacade facade = context.getBean(GymFacade.class);
        UserDao userDao = context.getBean(UserDao.class);
        TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        TrainingType cardio = new TrainingType(null, "CARDIO");
        Trainer trainer = trainerService.create(new Trainer("John", "Doe", false, cardio));
        Trainee trainee = traineeService.create(
                new Trainee("Jane", "Smith", false, LocalDate.of(1995, 5, 20), "Kyiv"));
        Trainer duplicateName = trainerService.create(new Trainer("Jane", "Smith", false, cardio));
        LocalDate date = LocalDate.of(2026, 1, 15);

        // Act
        authenticationService.authenticate(trainee.getUsername(), trainee.getPassword());
        List<Boolean> userQueries = transaction.execute(status -> List.of(
                userDao.existsByUsername(trainee.getUsername()),
                userDao.existsByUsername(trainer.getUsername()),
                userDao.existsByUsername("Missing.User"),
                userDao.findByUsername(trainee.getUsername()).orElseThrow() instanceof Trainee,
                userDao.findByUsername(trainer.getUsername()).orElseThrow() instanceof Trainer));
        List<String> unassignedBefore = trainerService.selectNotAssignedToTrainee(trainee.getUsername())
                .stream().map(Trainer::getUsername).toList();
        Training training = trainingService.addTraining(new NewTraining(
                trainee.getUsername(), trainer.getUsername(), "Morning run", null, date, 45));
        List<Training> traineeTrainings = trainingService.selectTraineeTrainings(trainee.getUsername(),
                new TraineeTrainingCriteria(date.minusDays(1), date.plusDays(1), "john doe", "cardio"));
        List<Training> trainerTrainings = trainingService.selectTrainerTrainings(trainer.getUsername(),
                new TrainerTrainingCriteria(date.minusDays(1), date.plusDays(1), "jane smith"));
        List<String> unassignedAfter = trainerService.selectNotAssignedToTrainee(trainee.getUsername())
                .stream().map(Trainer::getUsername).toList();
        Trainee assigned = traineeService.updateTrainers(
                trainee.getUsername(), List.of(trainer.getUsername(), duplicateName.getUsername()));
        Trainer loadedTrainer = trainerService.selectByUsername(trainer.getUsername());
        Trainee changes = new Trainee("Janet", "Smith", false, LocalDate.of(1995, 5, 20), "Lviv");
        changes.setUsername("Other.Trainee");
        Trainee updated = facade.updateTrainee(trainee.getUsername(), trainee.getPassword(), changes);
        facade.changeTraineePassword(trainee.getUsername(), trainee.getPassword(), "newPassword1");
        authenticationService.authenticate(trainee.getUsername(), "newPassword1");
        Trainer trainerChanges = new Trainer("Johnny", "Doe", false, new TrainingType(null, "YOGA"));
        trainerChanges.setUsername("Other.Trainer");
        facade.updateTrainer(trainer.getUsername(), trainer.getPassword(), trainerChanges);
        facade.changeTrainerPassword(trainer.getUsername(), trainer.getPassword(), "trainerPassword1");
        authenticationService.authenticate(trainer.getUsername(), "trainerPassword1");
        Trainer updatedTrainer = trainerService.selectByUsername(trainer.getUsername());
        boolean inactive = traineeService.toggleActive(trainee.getUsername());
        boolean activeAgain = traineeService.toggleActive(trainee.getUsername());
        long joinedTraineeRows = queryLong("SELECT count(*) FROM users u JOIN trainee t ON t.id = u.id"
                + " WHERE u.username = 'Jane.Smith' AND u.first_name = 'Janet'");
        traineeService.deleteByUsername(trainee.getUsername());

        // Assert
        assertEquals("Jane.Smith1", duplicateName.getUsername());
        assertTrue(trainer.getActive());
        assertTrue(trainee.getActive());
        assertNotEquals(trainer.getId(), trainee.getId());
        assertNotEquals(trainee.getId(), duplicateName.getId());
        assertEquals(List.of(true, true, false, true, true), userQueries);
        assertEquals(List.of("Jane.Smith1", "John.Doe"), unassignedBefore);
        assertEquals(List.of("Jane.Smith1"), unassignedAfter);
        assertEquals(trainee.getUsername(), training.getTrainee().getUsername());
        assertEquals(List.of(training.getId()), traineeTrainings.stream().map(Training::getId).toList());
        assertEquals(List.of(training.getId()), trainerTrainings.stream().map(Training::getId).toList());
        assertEquals(2, assigned.getTrainers().size());
        assertEquals(List.of("Jane.Smith"),
                loadedTrainer.getTrainees().stream().map(Trainee::getUsername).toList());
        assertEquals("Janet", updated.getFirstName());
        assertEquals("Lviv", updated.getAddress());
        assertEquals("Johnny", updatedTrainer.getFirstName());
        assertEquals("YOGA", updatedTrainer.getSpecialization().getTrainingTypeName());
        assertFalse(inactive);
        assertTrue(activeAgain);
        assertEquals(1, joinedTraineeRows);
        assertEquals(0, queryLong("SELECT count(*) FROM users WHERE username = 'Jane.Smith'"));
        assertEquals(0, queryLong("SELECT count(*) FROM training"));
        assertEquals(0, queryLong("SELECT count(*) FROM trainee_trainer"));
        assertEquals(2, queryLong("SELECT count(*) FROM users u JOIN trainer t ON t.id = u.id"));
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("Jane.Smith", "newPassword1"));
    }

    @Test
    void validatesInheritedUserFieldsWhenPersistingAProfile() throws SQLException {
        // Arrange
        AnnotationConfigApplicationContext context = newApplicationContext();
        context.refresh();
        Trainee trainee = new Trainee(" ", "Smith", true, null, null);
        trainee.setUsername("Jane.Smith");
        trainee.setPassword("password");
        SessionFactory sessionFactory = context.getBean(SessionFactory.class);
        TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        // Act
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class,
                () -> transaction.executeWithoutResult(status -> sessionFactory.getCurrentSession().persist(trainee)));

        // Assert
        assertTrue(exception.getConstraintViolations().stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("firstName")));
        assertEquals(0, queryLong("SELECT count(*) FROM users"));
        assertEquals(0, queryLong("SELECT count(*) FROM trainee"));
    }

    @Test
    void stopsStartupIfTheExistingTrainingTypeTableIsIncompatible() throws SQLException {
        // Arrange
        execute("CREATE TABLE training_type (id BIGINT PRIMARY KEY)");
        AnnotationConfigApplicationContext context = newApplicationContext();

        // Act
        BeanCreationException exception = assertThrows(BeanCreationException.class, context::refresh);

        // Assert
        assertTrue(exception.getMostSpecificCause().getMessage().contains("training_type_name"));
        assertEquals(0, queryLong("SELECT count(*) FROM information_schema.tables"
                + " WHERE table_schema = current_schema() AND table_name = 'users'"));
    }

    private AnnotationConfigApplicationContext newApplicationContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("migration-test", Map.of(
                "db.url", schemaUrl,
                "db.username", username,
                "db.password", password,
                "db.pool.max-size", "2",
                "hibernate.show_sql", "false",
                "hibernate.format_sql", "false")));
        context.register(AppConfig.class);
        contexts.add(context);
        return context;
    }

    private void captureTypesBeforeHibernate(AnnotationConfigApplicationContext context,
                                             AtomicReference<List<String>> types) {
        context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof LocalSessionFactoryBean && beanName.equals("sessionFactory")) {
                    try {
                        types.set(trainingTypeNames());
                    } catch (SQLException exception) {
                        throw new IllegalStateException("Could not read training types before Hibernate", exception);
                    }
                }
                return bean;
            }
        });
    }

    private List<String> trainingTypeNames() throws SQLException {
        try (Connection connection = schemaDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT training_type_name FROM training_type ORDER BY training_type_name")) {
            List<String> names = new ArrayList<>();
            while (results.next()) {
                names.add(results.getString(1));
            }
            return names;
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = schemaDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long queryLong(String sql) throws SQLException {
        try (Connection connection = schemaDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            if (!results.next()) {
                throw new IllegalStateException("Query returned no rows: " + sql);
            }
            return results.getLong(1);
        }
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new IllegalStateException(name + " is required for PostgreSQL migration tests");
        }
        return value;
    }
}
