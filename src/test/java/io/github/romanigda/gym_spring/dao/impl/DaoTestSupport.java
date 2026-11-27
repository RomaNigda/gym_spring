package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.domain.Trainee;
import io.github.romanigda.gym_spring.domain.Trainer;
import io.github.romanigda.gym_spring.domain.Training;
import io.github.romanigda.gym_spring.domain.TrainingType;
import io.github.romanigda.gym_spring.domain.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

/** In-memory H2 database with Hibernate-generated schema; DAOs run in a thread-bound "current session". */
final class DaoTestSupport implements AutoCloseable {
    private final SessionFactory sessionFactory;

    DaoTestSupport() {
        sessionFactory = new Configuration()
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.current_session_context_class", "thread")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Trainee.class)
                .addAnnotatedClass(Trainer.class)
                .addAnnotatedClass(Training.class)
                .addAnnotatedClass(TrainingType.class)
                .buildSessionFactory();
    }

    SessionFactory sessionFactory() {
        return sessionFactory;
    }

    Session session() {
        return sessionFactory.getCurrentSession();
    }

    <T> T inTransaction(Supplier<T> work) {
        Session session = session();
        session.beginTransaction();
        try {
            T result = work.get();
            session.getTransaction().commit();
            return result;
        } catch (RuntimeException e) {
            session.getTransaction().rollback();
            throw e;
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    TrainingType persistTrainingType(String name) {
        session().createNativeMutationQuery("insert into training_type (training_type_name) values (:name)")
                .setParameter("name", name)
                .executeUpdate();
        return session().createQuery("from TrainingType where trainingTypeName = :name", TrainingType.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    Trainer persistTrainer(String firstName, String lastName, String username, TrainingType type) {
        Trainer trainer = new Trainer(firstName, lastName, true, type);
        trainer.setUsername(username);
        trainer.setPassword("secret");
        session().persist(trainer);
        return trainer;
    }

    Trainee persistTrainee(String firstName, String lastName, String username) {
        Trainee trainee = newTrainee(firstName, lastName, username);
        session().persist(trainee);
        return trainee;
    }

    Training persistTraining(Trainee trainee, Trainer trainer, String name, TrainingType type, LocalDate date) {
        Training training = new Training(trainee, trainer, name, type, date, 30);
        session().persist(training);
        return training;
    }

    static Trainee newTrainee(String firstName, String lastName, String username) {
        Trainee trainee = new Trainee(firstName, lastName, true, LocalDate.of(1990, 1, 1), "Street 1");
        trainee.setUsername(username);
        trainee.setPassword("secret");
        return trainee;
    }

    @Override
    public void close() {
        sessionFactory.close();
    }
}
