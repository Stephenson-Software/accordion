package com.accord.controller;

import com.accord.model.Channel;
import com.accord.service.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequestMapping("/api/channels")
public class ChannelController {

    @Value("${app.channel.name-max-length:50}")
    private int maxChannelNameLength;

    @Value("${app.channel.name-min-length:3}")
    private int minChannelNameLength;

    @Autowired
    private ChannelService channelService;

    @GetMapping
    public ResponseEntity<List<Channel>> getAllChannels() {
        List<Channel> channels = channelService.getAllChannels();
        return ResponseEntity.ok(channels);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Channel> getChannelById(@PathVariable Long id) {
        return channelService.getChannelById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createChannel(@RequestBody Map<String, String> payload) {
        try {
            String name = payload.get("name");
            String description = payload.get("description");
            String createdBy = payload.get("createdBy");

            // Validate name
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Channel name is required"));
            }

            name = name.trim();
            if (name.length() < minChannelNameLength || name.length() > maxChannelNameLength) {
                return ResponseEntity.badRequest().body(Map.of("error", 
                    "Channel name must be between " + minChannelNameLength + " and " + maxChannelNameLength + " characters"));
            }

            // Validate name format (alphanumeric, hyphens, underscores)
            if (!name.matches("^[a-zA-Z0-9_-]+$")) {
                return ResponseEntity.badRequest().body(Map.of("error", 
                    "Channel name can only contain letters, numbers, hyphens, and underscores"));
            }

            // Validate description length (matches JPA column limit)
            if (description != null && description.length() > 500) {
                return ResponseEntity.badRequest().body(Map.of("error", 
                    "Channel description cannot exceed 500 characters"));
            }

            if (createdBy == null || createdBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Creator username is required"));
            }

            Channel channel = channelService.createChannel(name, description, createdBy.trim());
            return ResponseEntity.status(HttpStatus.CREATED).body(channel);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create channel"));
        }
    }
}
