package com.accord.controller;

import com.accord.dto.AuthResponse;
import com.accord.dto.LoginRequest;
import com.accord.dto.RegisterRequest;
import com.accord.model.User;
import com.accord.security.JwtUtil;
import com.accord.service.UserService;
import com.accord.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User authentication controller handling registration and login.
 * 
 * SECURITY NOTE: Rate Limiting
 * ----------------------------------------------------------------------------
 * This controller lacks rate limiting protection, making it vulnerable to:
 * - Username enumeration attacks
 * - Brute force password attacks
 * - Resource exhaustion through repeated requests
 * 
 * For production deployments, consider implementing rate limiting using:
 * - Spring Security's built-in rate limiting (Spring Security 6.2+)
 * - Bucket4j library for token bucket rate limiting
 * - API Gateway rate limiting (e.g., AWS API Gateway, Kong)
 * - Web Application Firewall (WAF) with rate limiting rules
 * 
 * Recommended limits:
 * - 5-10 failed login attempts per username per 15 minutes
 * - 10-20 registration attempts per IP address per hour
 * - Consider CAPTCHA after repeated failures
 * ----------------------------------------------------------------------------
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class UserController {

    @Value("${app.username.max-length}")
    private int maxUsernameLength;

    @Value("${app.username.min-length}")
    private int minUsernameLength;

    @Value("${app.password.min-length}")
    private int minPasswordLength;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        
        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid username"));
        }
        
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }
        
        if (!ValidationUtils.isValidPassword(password, minPasswordLength)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least " + minPasswordLength + 
                " characters and contain uppercase, lowercase, and digit"));
        }
        
        try {
            User user = userService.registerUser(username.trim(), password);
            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }
        
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username.trim(), password)
            );
            
            User user = userService.findByUsername(username.trim())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            
            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getId()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }

    @GetMapping("/check/{username}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable String username) {
        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            return ResponseEntity.badRequest().body(false);
        }
        boolean exists = userService.userExists(username.trim());
        return ResponseEntity.ok(exists);
    }

    @GetMapping
    public ResponseEntity<?> listUsers() {
        List<Map<String, Object>> users = userService.findAllUsers().stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    return userMap;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
