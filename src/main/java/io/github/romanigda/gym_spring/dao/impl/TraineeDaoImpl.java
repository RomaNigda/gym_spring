package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.TraineeDao;
import io.github.romanigda.gym_spring.domain.Trainee;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TraineeDaoImpl implements TraineeDao {
    private static final Logger log = LoggerFactory.getLogger(TraineeDaoImpl.class);

    private final SessionFactory sessionFactory;

    public TraineeDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Trainee save(Trainee trainee) {
        sessionFactory.getCurrentSession().persist(trainee);
        log.debug("Persisted trainee with id {}", trainee.getId());
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        Trainee merged = sessionFactory.getCurrentSession().merge(trainee);
        log.debug("Persisted trainee update with id {}", merged.getId());
        return merged;
    }

    @Override
    public void delete(Trainee trainee) {
        sessionFactory.getCurrentSession().remove(trainee);
        log.debug("Deleted trainee with id {}", trainee.getId());
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery("""
                        select distinct t from Trainee t
                        left join fetch t.trainers tr
                        left join fetch tr.specialization
                        where t.username = :username
                        """, Trainee.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }
}
