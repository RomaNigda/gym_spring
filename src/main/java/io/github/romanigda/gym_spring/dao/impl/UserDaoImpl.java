package io.github.romanigda.gym_spring.dao.impl;

import io.github.romanigda.gym_spring.dao.UserDao;
import io.github.romanigda.gym_spring.domain.User;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {
    private final SessionFactory sessionFactory;

    public UserDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery("from User u where u.username = :username", User.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    @Override
    public boolean existsByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery("select count(u) from User u where u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0;
    }

    @Override
    public List<String> findUsernamesStartingWith(String prefix) {
        return sessionFactory.getCurrentSession()
                .createSelectionQuery(
                        "select u.username from User u where u.username like :prefix escape '\\'", String.class)
                .setParameter("prefix", escapeLike(prefix) + "%")
                .getResultList();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
