package com.accord.controller;

import com.accord.model.DirectMessage;
import com.accord.model.User;
import com.accord.service.DirectMessageService;
import com.accord.service.UserService;
import com.accord.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dm")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class DirectMessageController {

    private static final int MAX_LIMIT = 500;

    @Value("${app.message.max-length}")
    private int maxMessageLength;

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send")
    public ResponseEntity<?> sendDirectMessage(@RequestBody Map<String, Object> payload) {
        String senderUsername = (String) payload.get("senderUsername");
        String recipientUsername = (String) payload.get("recipientUsername");
        String content = (String) payload.get("content");

        if (senderUsername == null || senderUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sender username is required"));
        }
        if (recipientUsername == null || recipientUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient username is required"));
        }
        if (!ValidationUtils.isValidContent(content, maxMessageLength)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid message content"));
        }
        if (senderUsername.trim().equals(recipientUsername.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot send a message to yourself"));
        }

        Optional<User> sender = userService.findByUsername(senderUsername.trim());
        Optional<User> recipient = userService.findByUsername(recipientUsername.trim());

        if (sender.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sender not found"));
        }
        if (recipient.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient not found"));
        }

        DirectMessage message = directMessageService.sendMessage(
                sender.get().getId(), recipient.get().getId(), content.trim());

        // Send real-time notification to recipient via WebSocket
        messagingTemplate.convertAndSend(
                "/user/" + recipient.get().getId() + "/queue/messages", message);

        return ResponseEntity.ok(message);
    }

    @GetMapping("/conversation")
    public ResponseEntity<?> getConversation(
            @RequestParam String user1,
            @RequestParam String user2,
            @RequestParam(defaultValue = "50") int limit) {

        if (user1 == null || user1.trim().isEmpty() || user2 == null || user2.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Both usernames are required"));
        }

        Optional<User> userOne = userService.findByUsername(user1.trim());
        Optional<User> userTwo = userService.findByUsername(user2.trim());

        if (userOne.isEmpty() || userTwo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        if (limit < 1) {
            limit = 1;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }

        List<DirectMessage> messages = directMessageService.getConversation(
                userOne.get().getId(), userTwo.get().getId(), limit);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/read/{messageId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long messageId,
                                        @RequestParam String username) {
        Optional<User> user = userService.findByUsername(username.trim());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        directMessageService.markAsRead(messageId, user.get().getId());
        return ResponseEntity.ok(Map.of("status", "marked as read"));
    }

    @PostMapping("/read/conversation")
    public ResponseEntity<?> markConversationAsRead(@RequestBody Map<String, String> payload) {
        String recipientUsername = payload.get("recipientUsername");
        String senderUsername = payload.get("senderUsername");

        if (recipientUsername == null || senderUsername == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Both usernames are required"));
        }

        Optional<User> recipient = userService.findByUsername(recipientUsername.trim());
        Optional<User> sender = userService.findByUsername(senderUsername.trim());

        if (recipient.isEmpty() || sender.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        directMessageService.markConversationAsRead(recipient.get().getId(), sender.get().getId());
        return ResponseEntity.ok(Map.of("status", "conversation marked as read"));
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadCount(@RequestParam String username) {
        Optional<User> user = userService.findByUsername(username.trim());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        long count = directMessageService.getUnreadCount(user.get().getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @GetMapping("/unread/from")
    public ResponseEntity<?> getUnreadCountFromSender(
            @RequestParam String username,
            @RequestParam String senderUsername) {
        Optional<User> user = userService.findByUsername(username.trim());
        Optional<User> sender = userService.findByUsername(senderUsername.trim());

        if (user.isEmpty() || sender.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        long count = directMessageService.getUnreadCountFromSender(
                user.get().getId(), sender.get().getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
