package io.github.romanigda.gym_spring.util;

import io.github.romanigda.gym_spring.dao.UserDao;
import io.github.romanigda.gym_spring.domain.User;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class UserCredentialsGenerator {
    private final UserDao userDao;

    public UserCredentialsGenerator(UserDao userDao) {
        this.userDao = userDao;
    }

    /** Creates an active user with a unique username (checked against the DB) and a random password. */
    public User createUser(String firstName, String lastName) {
        String baseUsername = firstName + "." + lastName;
        String username = UsernameGenerator.generateUsername(
                firstName, lastName, new HashSet<>(userDao.findUsernamesStartingWith(baseUsername)));
        User user = new User(firstName, lastName, true);
        user.setUsername(username);
        user.setPassword(PasswordGenerator.generatePassword());
        return user;
    }
}
