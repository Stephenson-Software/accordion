package com.accordion.service;

import com.accordion.config.AppConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP client for communicating with the Accordion backend REST API.
 * Handles authentication, channel management, and message retrieval.
 */
public class ApiClient {
    private static final Logger LOGGER = Logger.getLogger(ApiClient.class.getName());
    private static final Gson gson = new Gson();
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    private String jwtToken;

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    /**
     * Register a new user account.
     * @return AuthResponse with token, username, userId
     * @throws ApiException on failure
     */
    public AuthResponse register(String username, String password) throws ApiException {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        JsonObject response = postJson("/api/users/register", body, false);
        String token = response.get("token").getAsString();
        String respUsername = response.get("username").getAsString();
        long userId = response.get("userId").getAsLong();

        this.jwtToken = token;
        return new AuthResponse(token, respUsername, userId);
    }

    /**
     * Login with existing credentials.
     * @return AuthResponse with token, username, userId
     * @throws ApiException on failure
     */
    public AuthResponse login(String username, String password) throws ApiException {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        JsonObject response = postJson("/api/users/login", body, false);
        String token = response.get("token").getAsString();
        String respUsername = response.get("username").getAsString();
        long userId = response.get("userId").getAsLong();

        this.jwtToken = token;
        return new AuthResponse(token, respUsername, userId);
    }

    /**
     * Get all available channels.
     * @return List of ChannelInfo objects
     * @throws ApiException on failure
     */
    public List<ChannelInfo> getChannels() throws ApiException {
        String responseStr = getRequest("/api/channels");
        try {
            JsonArray array = gson.fromJson(responseStr, JsonArray.class);
            if (array == null) {
                throw new ApiException("Invalid server response: failed to parse channel list", 0);
            }
            List<ChannelInfo> channels = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                channels.add(parseChannel(obj));
            }
            return channels;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse channels response", e);
            throw new ApiException("Failed to parse server response: " + e.getMessage(), 0);
        }
    }

    /**
     * Create a new channel.
     * @return The created ChannelInfo
     * @throws ApiException on failure
     */
    public ChannelInfo createChannel(String name, String description, String createdBy) throws ApiException {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        if (description != null && !description.isEmpty()) {
            body.addProperty("description", description);
        }
        body.addProperty("createdBy", createdBy);

        JsonObject response = postJson("/api/channels", body, true);
        return parseChannel(response);
    }

    /**
     * Get recent messages for a channel.
     * @return List of MessageInfo objects
     * @throws ApiException on failure
     */
    public List<MessageInfo> getMessages(long channelId, int limit) throws ApiException {
        String responseStr = getRequest("/api/messages?channelId=" + channelId + "&limit=" + limit);
        try {
            JsonArray array = gson.fromJson(responseStr, JsonArray.class);
            if (array == null) {
                throw new ApiException("Invalid server response: failed to parse message list", 0);
            }
            List<MessageInfo> messages = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                messages.add(new MessageInfo(
                    obj.has("username") ? obj.get("username").getAsString() : "Unknown",
                    obj.has("content") ? obj.get("content").getAsString() : "",
                    obj.has("timestamp") ? obj.get("timestamp").getAsString() : "",
                    obj.has("channelId") && !obj.get("channelId").isJsonNull() ? obj.get("channelId").getAsLong() : 0
                ));
            }
            return messages;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse messages response", e);
            throw new ApiException("Failed to parse server response: " + e.getMessage(), 0);
        }
    }

    private ChannelInfo parseChannel(JsonObject obj) {
        return new ChannelInfo(
            obj.get("id").getAsLong(),
            obj.get("name").getAsString(),
            obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "",
            obj.has("createdBy") ? obj.get("createdBy").getAsString() : ""
        );
    }

    private JsonObject postJson(String path, JsonObject body, boolean requireAuth) throws ApiException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(AppConfig.getBackendUrl() + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setDoOutput(true);

            if (requireAuth && jwtToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
            }

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                String responseBody = readStream(connection.getInputStream());
                return gson.fromJson(responseBody, JsonObject.class);
            } else {
                String errorBody = readStream(connection.getErrorStream());
                String errorMessage = "Request failed";
                try {
                    JsonObject errorJson = gson.fromJson(errorBody, JsonObject.class);
                    if (errorJson.has("error")) {
                        errorMessage = errorJson.get("error").getAsString();
                    }
                } catch (Exception e) {
                    errorMessage = "Server error (HTTP " + responseCode + ")";
                }
                throw new ApiException(errorMessage, responseCode);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "API request failed: " + path, e);
            throw new ApiException("Could not connect to server: " + e.getMessage(), 0);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getRequest(String path) throws ApiException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(AppConfig.getBackendUrl() + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            if (jwtToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return readStream(connection.getInputStream());
            } else if (responseCode == 401 || responseCode == 403) {
                throw new ApiException("Authentication failed", responseCode);
            } else {
                throw new ApiException("Server error (HTTP " + responseCode + ")", responseCode);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "API request failed: " + path, e);
            throw new ApiException("Could not connect to server: " + e.getMessage(), 0);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
            return sb.toString();
        }
    }

    /**
     * Authentication response from login/register.
     */
    public static class AuthResponse {
        public final String token;
        public final String username;
        public final long userId;

        public AuthResponse(String token, String username, long userId) {
            this.token = token;
            this.username = username;
            this.userId = userId;
        }
    }

    /**
     * Channel information.
     */
    public static class ChannelInfo {
        public final long id;
        public final String name;
        public final String description;
        public final String createdBy;

        public ChannelInfo(long id, String name, String description, String createdBy) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.createdBy = createdBy;
        }
    }

    /**
     * Message information.
     */
    public static class MessageInfo {
        public final String username;
        public final String content;
        public final String timestamp;
        public final long channelId;

        public MessageInfo(String username, String content, String timestamp, long channelId) {
            this.username = username;
            this.content = content;
            this.timestamp = timestamp;
            this.channelId = channelId;
        }
    }

    /**
     * API error with HTTP status code.
     */
    public static class ApiException extends Exception {
        public final int statusCode;

        public ApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
