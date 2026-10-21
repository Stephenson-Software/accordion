package com.accordion.dto;

import com.accordion.config.JacksonConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void testAuthResponseCreation_NoArgs() {
        AuthResponse response = new AuthResponse();

        assertNull(response.getToken());
        assertNull(response.getUsername());
        assertNull(response.getUserId());
    }

    @Test
    void testAuthResponseCreation_WithArgs() {
        AuthResponse response = new AuthResponse("jwt-token", "alice", 7L);

        assertEquals("jwt-token", response.getToken());
        assertEquals("alice", response.getUsername());
        assertEquals(7L, response.getUserId());
    }

    @Test
    void testAuthResponseSetters_OverrideConstructorValues() {
        AuthResponse response = new AuthResponse("jwt-token", "alice", 7L);

        response.setToken("other-token");
        response.setUsername("bob");
        response.setUserId(9L);

        assertEquals("other-token", response.getToken());
        assertEquals("bob", response.getUsername());
        assertEquals(9L, response.getUserId());
    }

    @Test
    void testSerializesWithPropertyNamesTheWebClientReads() throws Exception {
        // index.html stores `token`, `username`, and `userId` from the
        // register/login response body in sessionStorage.
        AuthResponse response = new AuthResponse("jwt-token", "alice", 7L);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertEquals(3, json.size(), "Unexpected properties in: " + json);
        assertEquals("jwt-token", json.get("token").asText());
        assertEquals("alice", json.get("username").asText());
        assertEquals(7L, json.get("userId").asLong());
    }

    @Test
    void testCanBeRoundTripped() throws Exception {
        AuthResponse original = new AuthResponse("jwt-token", "alice", 7L);

        AuthResponse deserialized = objectMapper.readValue(
            objectMapper.writeValueAsString(original), AuthResponse.class);

        assertEquals("jwt-token", deserialized.getToken());
        assertEquals("alice", deserialized.getUsername());
        assertEquals(7L, deserialized.getUserId());
    }
}
