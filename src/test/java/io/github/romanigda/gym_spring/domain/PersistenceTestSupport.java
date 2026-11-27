package io.github.romanigda.gym_spring.domain;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/** In-memory database with the schema generated from the entities, for behaviour-level mapping tests. */
final class PersistenceTestSupport implements AutoCloseable {
    private final SessionFactory sessionFactory;

    PersistenceTestSupport() {
        sessionFactory = new Configuration()
                .setProperty("hibernate.connection.url",
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Trainee.class)
                .addAnnotatedClass(Trainer.class)
                .addAnnotatedClass(Training.class)
                .addAnnotatedClass(TrainingType.class)
                .buildSessionFactory();
    }

    void inTransaction(Consumer<Session> work) {
        sessionFactory.inTransaction(work);
    }

    <T> T fromTransaction(Function<Session, T> work) {
        return sessionFactory.fromTransaction(work);
    }

    static TrainingType persistTrainingType(Session session) {
        session.createNativeMutationQuery(
                "insert into training_type (training_type_name) values ('YOGA')").executeUpdate();
        return session.createQuery("from TrainingType", TrainingType.class).getSingleResult();
    }

    static Trainee newTrainee(String username) {
        Trainee trainee = new Trainee("John", "Doe", true, LocalDate.of(1990, 1, 1), "Street 1");
        trainee.setUsername(username);
        trainee.setPassword("secret");
        return trainee;
    }

    static Trainer newTrainer(String username, TrainingType type) {
        Trainer trainer = new Trainer("Jane", "Roe", true, type);
        trainer.setUsername(username);
        trainer.setPassword("secret");
        return trainer;
    }

    @Override
    public void close() {
        sessionFactory.close();
    }
}
