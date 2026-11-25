package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.TrainerDao;
import io.github.romanigda.gym_spring.domain.Trainer;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class TrainerDaoImpl implements TrainerDao {
    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    private final SessionFactory sessionFactory;

    public TrainerDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Trainer save(Trainer trainer) {
        sessionFactory.getCurrentSession().persist(trainer);
        log.debug("Persisted trainer with id {}", trainer.getId());
        return trainer;
    }

    @Override
    public Trainer update(Trainer trainer) {
        Trainer merged = sessionFactory.getCurrentSession().merge(trainer);
        log.debug("Persisted trainer update with id {}", merged.getId());
        return merged;
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery("""
                        select distinct t from Trainer t
                        join fetch t.specialization
                        left join fetch t.trainees te
                        where t.username = :username
                        """, Trainer.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    @Override
    public List<Trainer> findByUsernames(Collection<String> usernames) {
        if (usernames.isEmpty()) {
            return List.of();
        }
        return sessionFactory.getCurrentSession()
                .createSelectionQuery("""
                        select t from Trainer t
                        join fetch t.specialization
                        where t.username in :usernames
                        """, Trainer.class)
                .setParameterList("usernames", usernames)
                .getResultList();
    }

    @Override
    public List<Trainer> findTrainersNotAssignedToTrainee(String traineeUsername) {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery("""
                        select t from Trainer t
                        join fetch t.specialization
                        where not exists (
                            select 1 from Trainee te join te.trainers assigned
                            where te.username = :traineeUsername and assigned = t
                        )
                        order by t.username
                        """, Trainer.class)
                .setParameter("traineeUsername", traineeUsername)
                .getResultList();
    }
}
