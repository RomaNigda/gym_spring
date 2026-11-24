package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.TrainingDao;
import io.github.romanigda.gym_spring.domain.Training;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class TrainingDaoImpl implements TrainingDao {
    private static final Logger log = LoggerFactory.getLogger(TrainingDaoImpl.class);

    private static final String BASE_QUERY = """
            select tr from Training tr
            join fetch tr.trainee te
            join fetch te.user teu
            join fetch tr.trainer t
            join fetch t.user tu
            join fetch tr.trainingType tt
            where 1 = 1
            """;

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
        StringBuilder hql = new StringBuilder(BASE_QUERY).append(" and teu.username = :username");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("username", traineeUsername);
        appendDateRange(hql, params, fromDate, toDate);
        if (hasText(trainerName)) {
            hql.append(" and (lower(tu.username) like :trainerName")
                    .append(" or lower(concat(tu.firstName, ' ', tu.lastName)) like :trainerName)");
            params.put("trainerName", containsPattern(trainerName));
        }
        if (hasText(trainingTypeName)) {
            hql.append(" and lower(tt.trainingTypeName) = :trainingType");
            params.put("trainingType", trainingTypeName.trim().toLowerCase(Locale.ROOT));
        }
        return execute(hql, params);
    }

    @Override
    public List<Training> findTrainerTrainings(String trainerUsername, LocalDate fromDate, LocalDate toDate,
                                               String traineeName) {
        StringBuilder hql = new StringBuilder(BASE_QUERY).append(" and tu.username = :username");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("username", trainerUsername);
        appendDateRange(hql, params, fromDate, toDate);
        if (hasText(traineeName)) {
            hql.append(" and (lower(teu.username) like :traineeName")
                    .append(" or lower(concat(teu.firstName, ' ', teu.lastName)) like :traineeName)");
            params.put("traineeName", containsPattern(traineeName));
        }
        return execute(hql, params);
    }

    private List<Training> execute(StringBuilder hql, Map<String, Object> params) {
        hql.append(" order by tr.trainingDate");
        SelectionQuery<Training> query = sessionFactory.getCurrentSession()
                .createSelectionQuery(hql.toString(), Training.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    private static void appendDateRange(StringBuilder hql, Map<String, Object> params,
                                        LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            hql.append(" and tr.trainingDate >= :fromDate");
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            hql.append(" and tr.trainingDate <= :toDate");
            params.put("toDate", toDate);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String containsPattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
