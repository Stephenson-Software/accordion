package com.accordion.controller;

import com.accordion.model.User;
import com.accordion.service.UserService;
import com.accordion.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class UserController {

    @Value("${app.username.max-length}")
    private int maxUsernameLength;

    @Value("${app.username.min-length}")
    private int minUsernameLength;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            return ResponseEntity.badRequest().build();
        }
        
        User user = userService.createOrGetUser(username.trim());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/check/{username}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable String username) {
        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            return ResponseEntity.badRequest().body(false);
        }
        boolean exists = userService.userExists(username.trim());
        return ResponseEntity.ok(exists);
    }
}
