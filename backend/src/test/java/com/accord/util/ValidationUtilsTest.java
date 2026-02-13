package com.accord.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void testValidUsername() {
        assertTrue(ValidationUtils.isValidUsername("john123", 3, 50));
        assertTrue(ValidationUtils.isValidUsername("user_name", 3, 50));
        assertTrue(ValidationUtils.isValidUsername("JohnDoe", 3, 50));
        assertTrue(ValidationUtils.isValidUsername("a1b2c3", 3, 50));
    }

    @Test
    void testInvalidUsername_Null() {
        assertFalse(ValidationUtils.isValidUsername(null, 3, 50));
    }

    @Test
    void testInvalidUsername_Empty() {
        assertFalse(ValidationUtils.isValidUsername("", 3, 50));
        assertFalse(ValidationUtils.isValidUsername("  ", 3, 50));
    }

    @Test
    void testInvalidUsername_TooShort() {
        assertFalse(ValidationUtils.isValidUsername("ab", 3, 50));
        assertFalse(ValidationUtils.isValidUsername("a", 3, 50));
    }

    @Test
    void testInvalidUsername_TooLong() {
        String longUsername = "a".repeat(51);
        assertFalse(ValidationUtils.isValidUsername(longUsername, 3, 50));
    }

    @Test
    void testInvalidUsername_SpecialCharacters() {
        assertFalse(ValidationUtils.isValidUsername("user@name", 3, 50));
        assertFalse(ValidationUtils.isValidUsername("user name", 3, 50));
        assertFalse(ValidationUtils.isValidUsername("user-name", 3, 50));
        assertFalse(ValidationUtils.isValidUsername("user.name", 3, 50));
        assertFalse(ValidationUtils.isValidUsername("user!name", 3, 50));
    }

    @Test
    void testInvalidUsername_ControlCharacters() {
        assertFalse(ValidationUtils.isValidUsername("user\nname", 3, 50));
        assertFalse(ValidationUtils.isValidUsername("user\tname", 3, 50));
    }

    @Test
    void testUsername_EdgeCases() {
        // Exactly min length
        assertTrue(ValidationUtils.isValidUsername("abc", 3, 50));
        // Exactly max length
        String maxLengthUsername = "a".repeat(50);
        assertTrue(ValidationUtils.isValidUsername(maxLengthUsername, 3, 50));
    }

    @Test
    void testValidContent() {
        assertTrue(ValidationUtils.isValidContent("Hello world", 1000));
        assertTrue(ValidationUtils.isValidContent("Test message", 1000));
    }

    @Test
    void testInvalidContent_Null() {
        assertFalse(ValidationUtils.isValidContent(null, 1000));
    }

    @Test
    void testInvalidContent_Empty() {
        assertFalse(ValidationUtils.isValidContent("", 1000));
        assertFalse(ValidationUtils.isValidContent("   ", 1000));
    }

    @Test
    void testInvalidContent_TooLong() {
        String longContent = "a".repeat(1001);
        assertFalse(ValidationUtils.isValidContent(longContent, 1000));
    }

    @Test
    void testContent_EdgeCases() {
        // Exactly max length
        String maxLengthContent = "a".repeat(1000);
        assertTrue(ValidationUtils.isValidContent(maxLengthContent, 1000));
        
        // One character
        assertTrue(ValidationUtils.isValidContent("a", 1000));
    }
}
