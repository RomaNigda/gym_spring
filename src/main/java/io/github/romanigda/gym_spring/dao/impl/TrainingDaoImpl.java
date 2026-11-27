package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.domain.Training;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class TrainingDaoImpl implements TrainingDao {
    private static final Logger log = LoggerFactory.getLogger(TrainingDaoImpl.class);

    private final SessionFactory sessionFactory;

    public TrainingDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Training save(Training training) {
        sessionFactory.getCurrentSession().persist(training);
        log.debug("Persisted training with id {}", training.getId());
        return training;
    }

    @Override
    public List<Training> findTraineeTrainings(String traineeUsername, LocalDate fromDate, LocalDate toDate,
                                               String trainerName, String trainingTypeName) {
        return find(traineeUsername, "trainee", fromDate, toDate, (cb, ctx, predicates) -> {
            if (hasText(trainerName)) {
                predicates.add(nameMatches(cb, ctx.trainer(), containsPattern(trainerName)));
            }
            if (hasText(trainingTypeName)) {
                predicates.add(cb.equal(cb.lower(ctx.trainingType().get("trainingTypeName")),
                        trainingTypeName.trim().toLowerCase(Locale.ROOT)));
            }
        });
    }

    @Override
    public List<Training> findTrainerTrainings(String trainerUsername, LocalDate fromDate, LocalDate toDate,
                                               String traineeName) {
        return find(trainerUsername, "trainer", fromDate, toDate, (cb, ctx, predicates) -> {
            if (hasText(traineeName)) {
                predicates.add(nameMatches(cb, ctx.trainee(), containsPattern(traineeName)));
            }
        });
    }

    private List<Training> find(String ownerUsername, String ownerAttribute, LocalDate fromDate, LocalDate toDate,
                                ExtraFilters extraFilters) {
        CriteriaBuilder cb = sessionFactory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<Training> cq = cb.createQuery(Training.class);
        Root<Training> tr = cq.from(Training.class);
        Join<Training, ?> trainee = fetchJoin(tr, "trainee");
        Join<Training, ?> trainer = fetchJoin(tr, "trainer");
        Join<Training, ?> trainingType = fetchJoin(tr, "trainingType");

        List<Predicate> predicates = new ArrayList<>();
        Join<Training, ?> owner = "trainee".equals(ownerAttribute) ? trainee : trainer;
        predicates.add(cb.equal(owner.get("username"), ownerUsername));
        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(tr.<LocalDate>get("trainingDate"), fromDate));
        }
        if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(tr.<LocalDate>get("trainingDate"), toDate));
        }
        extraFilters.apply(cb, new Joins(trainee, trainer, trainingType), predicates);

        cq.select(tr).where(predicates.toArray(Predicate[]::new)).orderBy(cb.asc(tr.get("trainingDate")));
        return sessionFactory.getCurrentSession().createQuery(cq).getResultList();
    }

    @SuppressWarnings("unchecked")
    private static Join<Training, ?> fetchJoin(Root<Training> root, String attribute) {
        return (Join<Training, ?>) (Fetch<?, ?>) root.fetch(attribute, JoinType.INNER);
    }

    private static Predicate nameMatches(CriteriaBuilder cb, Join<Training, ?> user, String pattern) {
        return cb.or(
                cb.like(cb.lower(user.get("username")), pattern),
                cb.like(cb.lower(cb.concat(cb.concat(user.<String>get("firstName"), " "),
                        user.<String>get("lastName"))), pattern));
    }

    private record Joins(Join<Training, ?> trainee, Join<Training, ?> trainer, Join<Training, ?> trainingType) {
    }

    @FunctionalInterface
    private interface ExtraFilters {
        void apply(CriteriaBuilder cb, Joins ctx, List<Predicate> predicates);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String containsPattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
