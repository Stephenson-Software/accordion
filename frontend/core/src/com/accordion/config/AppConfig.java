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
    
    // Message Configuration
    public static final int MAX_MESSAGE_LENGTH = 1000;
    
    // Username Configuration
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final String USERNAME_PATTERN = "^[A-Za-z0-9_]+$";
    
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
}
