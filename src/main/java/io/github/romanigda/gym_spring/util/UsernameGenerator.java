package io.github.romanigda.gym_spring.util;

import java.util.Set;

public class UsernameGenerator {

    public static String generateUsername(String firstName, String lastName, Set<String> existingUsernames) {
        String baseUsername = firstName + "." + lastName;

        if (!existingUsernames.contains(baseUsername)) {
            return baseUsername;
        }

        int suffix = 1;
        String candidateUsername = baseUsername + suffix;

        while (existingUsernames.contains(candidateUsername)) {
            suffix++;
            candidateUsername = baseUsername + suffix;
        }

        return candidateUsername;
    }
}