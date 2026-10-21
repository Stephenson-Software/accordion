package com.accordion.dto;

import com.accordion.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void testLoginRequestCreation_NoArgs() {
        LoginRequest request = new LoginRequest();

        assertNull(request.getUsername());
        assertNull(request.getPassword());
    }

    @Test
    void testLoginRequestCreation_WithArgs() {
        LoginRequest request = new LoginRequest("alice", "Password1");

        assertEquals("alice", request.getUsername());
        assertEquals("Password1", request.getPassword());
    }

    @Test
    void testLoginRequestSetters_OverrideConstructorValues() {
        LoginRequest request = new LoginRequest("alice", "Password1");

        request.setUsername("bob");
        request.setPassword("Password2");

        assertEquals("bob", request.getUsername());
        assertEquals("Password2", request.getPassword());
    }

    @Test
    void testDeserializesTheBodyTheWebClientSends() throws Exception {
        // index.html posts `{ username, password }` to /api/users/login.
        LoginRequest request = objectMapper.readValue(
            "{\"username\":\"alice\",\"password\":\"Password1\"}", LoginRequest.class);

        assertEquals("alice", request.getUsername());
        assertEquals("Password1", request.getPassword());
    }

    @Test
    void testMissingFieldsDeserializeAsNull() throws Exception {
        // UserController.login rejects null credentials itself, so a partial
        // body must reach it rather than fail during deserialization.
        LoginRequest request = objectMapper.readValue(
            "{\"username\":\"alice\"}", LoginRequest.class);

        assertEquals("alice", request.getUsername());
        assertNull(request.getPassword());
    }
}
