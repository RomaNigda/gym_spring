package io.github.romanigda.gym_spring.config;

import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.domain.User;
import io.github.romanigda.gym_spring.service.TrainerService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                .create(new Trainer(new User("Test", "Trainer", true), new TrainingType(null, "CARDIO")));
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
