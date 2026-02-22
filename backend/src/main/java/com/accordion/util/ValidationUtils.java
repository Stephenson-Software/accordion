package com.accordion.util;

import java.util.regex.Pattern;

/**
 * Utility class for validating user input.
 * Centralizes validation logic to ensure consistency across controllers.
 */
public class ValidationUtils {
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    
    /**
     * Validates a username accordioning to application rules.
     * 
     * @param username the username to validate
     * @param minLength minimum allowed length
     * @param maxLength maximum allowed length
     * @return true if valid, false otherwise
     */
    public static boolean isValidUsername(String username, int minLength, int maxLength) {
        if (username == null) {
            return false;
        }
        String trimmed = username.trim();
        if (trimmed.length() < minLength || trimmed.length() > maxLength) {
            return false;
        }
        return USERNAME_PATTERN.matcher(trimmed).matches();
    }
    
    /**
     * Validates message content.
     * 
     * @param content the message content to validate
     * @param maxLength maximum allowed length
     * @return true if valid, false otherwise
     */
    public static boolean isValidContent(String content, int maxLength) {
        if (content == null) {
            return false;
        }
        return !content.trim().isEmpty() && content.length() <= maxLength;
    }
}
