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

    /** Generates a unique username (checked against the DB) and a random password for the user. */
    public void generateCredentials(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String baseUsername = firstName + "." + lastName;
        String username = UsernameGenerator.generateUsername(
                firstName, lastName, new HashSet<>(userDao.findUsernamesStartingWith(baseUsername)));
        user.setUsername(username);
        user.setPassword(PasswordGenerator.generatePassword());
    }
}
