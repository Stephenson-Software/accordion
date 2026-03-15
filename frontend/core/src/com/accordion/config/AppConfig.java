package com.accordion.config;

/**
 * Configuration class for application settings.
 * Provides centralized configuration for WebSocket URLs and other settings.
 */
public class AppConfig {
    
    // WebSocket Configuration
    // For production or remote servers, update this URL
    // Example: ws://yourserver.com:8080/ws
    private static final String DEFAULT_WEBSOCKET_URL = "ws://localhost:8080/ws";
    
    // Backend REST API Configuration
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080";
    
    // Message Configuration
    public static final int MAX_MESSAGE_LENGTH = 1000;
    
    // Username Configuration
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final String USERNAME_PATTERN = "^[A-Za-z0-9_]+$";
    
    // Password Configuration
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    // Channel Configuration
    public static final int MIN_CHANNEL_NAME_LENGTH = 3;
    public static final int MAX_CHANNEL_NAME_LENGTH = 50;
    public static final int MAX_CHANNEL_DESCRIPTION_LENGTH = 500;
    public static final String CHANNEL_NAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    
    // Typing Indicator Configuration
    public static final int TYPING_DEBOUNCE_MS = 2000;
    public static final int TYPING_TIMEOUT_MS = 3000;
    
    /**
     * Get the WebSocket server URL.
     * Can be overridden by system property 'accordion.websocket.url'
     */
    public static String getWebSocketUrl() {
        String systemProperty = System.getProperty("accordion.websocket.url");
        if (systemProperty != null && !systemProperty.isEmpty()) {
            return systemProperty;
        }
        return DEFAULT_WEBSOCKET_URL;
    }
    
    /**
     * Get the backend REST API URL.
     * Can be overridden by system property 'accordion.backend.url'
     */
    public static String getBackendUrl() {
        String systemProperty = System.getProperty("accordion.backend.url");
        if (systemProperty != null && !systemProperty.isEmpty()) {
            return systemProperty;
        }
        return DEFAULT_BACKEND_URL;
    }
}
