package io.github.romanigda.gym_spring.dao;

import io.github.romanigda.gym_spring.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Returns all usernames that start with the given prefix (used for serial-number generation). */
    List<String> findUsernamesStartingWith(String prefix);
}
