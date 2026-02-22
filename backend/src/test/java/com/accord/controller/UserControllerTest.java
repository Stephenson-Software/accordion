package com.accord.controller;

import com.accord.dto.LoginRequest;
import com.accord.dto.RegisterRequest;
import com.accord.model.User;
import com.accord.security.JwtUtil;
import com.accord.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
        classes = {com.accord.config.SecurityConfig.class, 
                   com.accord.security.JwtAuthenticationFilter.class,
                   com.accord.security.WebSocketAuthInterceptor.class}))
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "app.cors.allowed-origins=*",
    "app.username.max-length=50",
    "app.username.min-length=3",
    "app.password.min-length=8",
    "jwt.secret=TestSecretKeyMinimum32BytesForHS256AlgorithmCompatibility",
    "jwt.expiration=86400000"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser");
        testUser.setId(1L);
        testUser.setPassword("$2a$10$encoded.password.hash");
    }

    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "Password1");
        
        when(userService.registerUser(eq("testuser"), anyString())).thenReturn(testUser);
        when(jwtUtil.generateToken("testuser")).thenReturn("test.jwt.token");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test.jwt.token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(userService, times(1)).registerUser(eq("testuser"), eq("Password1"));
    }

    @Test
    void testRegister_InvalidUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("ab", "Password1");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(anyString(), anyString());
    }

    @Test
    void testRegister_InvalidPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "pass");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(anyString(), anyString());
    }

    @Test
    void testRegister_UsernameExists() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "Password1");
        
        when(userService.registerUser(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "Password1");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("testuser")).thenReturn("test.jwt.token");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test.jwt.token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "WrongPassword");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_NullUsername() throws Exception {
        LoginRequest request = new LoginRequest(null, "Password1");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void testLogin_NullPassword() throws Exception {
        LoginRequest request = new LoginRequest("testuser", null);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
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
