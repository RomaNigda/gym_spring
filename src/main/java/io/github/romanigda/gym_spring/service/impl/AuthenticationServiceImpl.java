package io.github.romanigda.gym_spring.service.impl;

import io.github.romanigda.gym_spring.dao.UserDao;
import io.github.romanigda.gym_spring.exception.AuthenticationException;
import io.github.romanigda.gym_spring.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final UserDao userDao;

    public AuthenticationServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    @Transactional(readOnly = true)
    public void authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Authentication failed: username or password is blank");
            throw new AuthenticationException("Username and password must not be blank");
        }
        boolean matches = userDao.findByUsername(username.trim())
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
        if (!matches) {
            log.warn("Authentication failed for username {}", username);
            throw new AuthenticationException("Invalid username or password");
        }
        log.debug("User {} authenticated", username);
    }
}
