package com.accord.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserCreation_NoArgs() {
        User user = new User();
        
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNotNull(user.getJoinedAt());
        assertTrue(user.getJoinedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testUserCreation_WithUsername() {
        User user = new User("testuser");
        
        assertNull(user.getId());
        assertEquals("testuser", user.getUsername());
        assertNotNull(user.getJoinedAt());
        assertTrue(user.getJoinedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testUserSetters() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        
        user.setId(1L);
        user.setUsername("newuser");
        user.setJoinedAt(now);
        
        assertEquals(1L, user.getId());
        assertEquals("newuser", user.getUsername());
        assertEquals(now, user.getJoinedAt());
    }

    @Test
    void testUserGetters() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User("testuser");
        user.setId(5L);
        user.setJoinedAt(now);
        
        assertEquals(5L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals(now, user.getJoinedAt());
    }
}
