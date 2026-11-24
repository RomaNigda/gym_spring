package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.TrainingTypeDao;
import io.github.romanigda.gym_spring.domain.TrainingType;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingTypeDaoImpl implements TrainingTypeDao {
    private final SessionFactory sessionFactory;

    public TrainingTypeDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<TrainingType> findById(Long id) {
        return Optional.ofNullable(sessionFactory.getCurrentSession().find(TrainingType.class, id));
    }

    @Override
    public Optional<TrainingType> findByName(String trainingTypeName) {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery(
                        "from TrainingType tt where lower(tt.trainingTypeName) = lower(:name)", TrainingType.class)
                .setParameter("name", trainingTypeName.trim())
                .uniqueResultOptional();
    }

    @Override
    public List<TrainingType> findAll() {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery("from TrainingType tt order by tt.id", TrainingType.class)
                .getResultList();
    }
}
