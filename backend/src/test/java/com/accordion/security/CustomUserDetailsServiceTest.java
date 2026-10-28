package com.accordion.security;

import com.accordion.model.User;
import com.accordion.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void testLoadUserByUsername_ExistingUserMapsToUserDetails() {
        User alice = new User("alice");
        alice.setPassword("$2a$10$encodedPasswordHash");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertEquals("alice", details.getUsername());
        assertEquals("$2a$10$encodedPasswordHash", details.getPassword());
        assertTrue(details.getAuthorities().isEmpty());
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    void testLoadUserByUsername_UnknownUserThrows() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("ghost"));

        assertEquals("User not found: ghost", exception.getMessage());
    }

    @Test
    void testLoadUserByUsername_UserWithoutPasswordThrows() {
        when(userRepository.findByUsername("legacy")).thenReturn(Optional.of(new User("legacy")));

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("legacy"));

        assertEquals("Password not configured for account: legacy. Please contact support or re-register.",
            exception.getMessage());
    }
}
