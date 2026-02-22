package com.accordion.service;

import com.accordion.model.User;
import com.accordion.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser");
        testUser.setId(1L);
    }

    @Test
    void testCreateOrGetUser_NewUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.createOrGetUser("newuser");

        assertNotNull(result);
        verify(userRepository, times(1)).findByUsername("newuser");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateOrGetUser_ExistingUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User result = userService.createOrGetUser("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_Success() {
        String rawPassword = "Password123";
        String encodedPassword = "$2a$10$encodedPasswordHash";
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User result = userService.registerUser("newuser", rawPassword);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameExists() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("existinguser", "Password123");
        });

        verify(userRepository, times(1)).existsByUsername("existinguser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testVerifyPassword_Correct() {
        String rawPassword = "Password123";
        String encodedPassword = "$2a$10$encodedPasswordHash";
        
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        assertTrue(result);
        verify(passwordEncoder, times(1)).matches(rawPassword, encodedPassword);
    }

    @Test
    void testVerifyPassword_Incorrect() {
        String rawPassword = "WrongPassword";
        String encodedPassword = "$2a$10$encodedPasswordHash";
        
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        assertFalse(result);
        verify(passwordEncoder, times(1)).matches(rawPassword, encodedPassword);
    }

    @Test
    void testUserExists_True() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        boolean result = userService.userExists("testuser");

        assertTrue(result);
        verify(userRepository, times(1)).existsByUsername("testuser");
    }

    @Test
    void testUserExists_False() {
        when(userRepository.existsByUsername("nonexistent")).thenReturn(false);

        boolean result = userService.userExists("nonexistent");

        assertFalse(result);
        verify(userRepository, times(1)).existsByUsername("nonexistent");
    }

    @Test
    void testFindByUsername_Found() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void testFindByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("nonexistent");

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }
}
