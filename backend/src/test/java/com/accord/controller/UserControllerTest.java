package com.accord.controller;

import com.accord.model.User;
import com.accord.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@TestPropertySource(properties = {
    "app.cors.allowed-origins=*",
    "app.username.max-length=50",
    "app.username.min-length=3"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser");
        testUser.setId(1L);
    }

    @Test
    void testLogin_Success() throws Exception {
        when(userService.createOrGetUser(anyString())).thenReturn(testUser);

        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService, times(1)).createOrGetUser("testuser");
    }

    @Test
    void testLogin_EmptyUsername() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createOrGetUser(anyString());
    }

    @Test
    void testLogin_NullUsername() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", null);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createOrGetUser(anyString());
    }

    @Test
    void testLogin_UsernameTooShort() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "ab");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createOrGetUser(anyString());
    }

    @Test
    void testLogin_UsernameTooLong() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "a".repeat(51));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createOrGetUser(anyString());
    }

    @Test
    void testLogin_InvalidCharacters() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "user@name");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createOrGetUser(anyString());
    }

    @Test
    void testCheckUsername_Exists() throws Exception {
        when(userService.userExists("testuser")).thenReturn(true);

        mockMvc.perform(get("/api/users/check/testuser"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService, times(1)).userExists("testuser");
    }

    @Test
    void testCheckUsername_NotExists() throws Exception {
        when(userService.userExists("newuser")).thenReturn(false);

        mockMvc.perform(get("/api/users/check/newuser"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService, times(1)).userExists("newuser");
    }

    @Test
    void testCheckUsername_Invalid() throws Exception {
        mockMvc.perform(get("/api/users/check/ab"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).userExists(anyString());
    }
}
