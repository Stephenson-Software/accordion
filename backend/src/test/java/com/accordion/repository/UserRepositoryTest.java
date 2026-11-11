package com.accordion.repository;

import com.accordion.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes the username lookups UserService and CustomUserDetailsService rely on,
 * including the database-level uniqueness constraint on usernames.
 * Runs against the embedded H2 database that @DataJpaTest provisions per test.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsernameReturnsSavedUser() {
        userRepository.save(new User("alice"));

        Optional<User> result = userRepository.findByUsername("alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        assertNotNull(result.get().getId());
    }

    @Test
    void findByUsernameReturnsEmptyForUnknownUser() {
        userRepository.save(new User("alice"));

        assertTrue(userRepository.findByUsername("bob").isEmpty());
    }

    @Test
    void findByUsernameIsCaseSensitive() {
        userRepository.save(new User("alice"));

        assertTrue(userRepository.findByUsername("ALICE").isEmpty());
    }

    @Test
    void existsByUsernameReflectsSavedUsers() {
        userRepository.save(new User("alice"));

        assertTrue(userRepository.existsByUsername("alice"));
        assertFalse(userRepository.existsByUsername("bob"));
    }

    @Test
    void existsByUsernameIsCaseSensitive() {
        userRepository.save(new User("alice"));

        assertFalse(userRepository.existsByUsername("Alice"));
    }

    @Test
    void savingDuplicateUsernameViolatesUniqueConstraint() {
        userRepository.saveAndFlush(new User("alice"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(new User("alice")));
    }
}
