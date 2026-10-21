package com.accordion.dto;

import com.accordion.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void testRegisterRequestCreation_NoArgs() {
        RegisterRequest request = new RegisterRequest();

        assertNull(request.getUsername());
        assertNull(request.getPassword());
    }

    @Test
    void testRegisterRequestCreation_WithArgs() {
        RegisterRequest request = new RegisterRequest("alice", "Password1");

        assertEquals("alice", request.getUsername());
        assertEquals("Password1", request.getPassword());
    }

    @Test
    void testRegisterRequestSetters_OverrideConstructorValues() {
        RegisterRequest request = new RegisterRequest("alice", "Password1");

        request.setUsername("bob");
        request.setPassword("Password2");

        assertEquals("bob", request.getUsername());
        assertEquals("Password2", request.getPassword());
    }

    @Test
    void testDeserializesTheBodyTheWebClientSends() throws Exception {
        // index.html posts `{ username, password }` to /api/users/register.
        RegisterRequest request = objectMapper.readValue(
            "{\"username\":\"alice\",\"password\":\"Password1\"}", RegisterRequest.class);

        assertEquals("alice", request.getUsername());
        assertEquals("Password1", request.getPassword());
    }

    @Test
    void testMissingFieldsDeserializeAsNull() throws Exception {
        // UserController.register reports a missing username or password
        // itself, so a partial body must reach it rather than fail during
        // deserialization.
        RegisterRequest request = objectMapper.readValue(
            "{\"password\":\"Password1\"}", RegisterRequest.class);

        assertNull(request.getUsername());
        assertEquals("Password1", request.getPassword());
    }
}
