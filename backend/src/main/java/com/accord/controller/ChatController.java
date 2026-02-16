package com.accord.controller;

import com.accord.model.ChatMessage;
import com.accord.model.TypingIndicator;
import com.accord.service.ChatService;
import com.accord.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    @Value("${app.message.max-length}")
    private int maxMessageLength;

    @Value("${app.username.max-length}")
    private int maxUsernameLength;

    @Value("${app.username.min-length}")
    private int minUsernameLength;

    @Autowired
    private ChatService chatService;

    @Autowired
    private com.accord.service.ChannelService channelService;

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(Map<String, String> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }

        String username = payload.get("username");
        String content = payload.get("content");
        
        // Legacy /chat.send always uses the default channel (ignore any channelId in payload)
        Long channelId = channelService.getOrCreateDefaultChannel().getId();

        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            throw new IllegalArgumentException("Invalid username");
        }
        if (!ValidationUtils.isValidContent(content, maxMessageLength)) {
            throw new IllegalArgumentException("Invalid message content");
        }
        
        // Trim username and content before saving to ensure consistency
        String trimmedUsername = username.trim();
        String trimmedContent = content.trim();
        
        return chatService.saveMessage(trimmedUsername, trimmedContent, channelId);
    }

    @MessageMapping("/chat.send/{channelId}")
    @SendTo("/topic/messages/{channelId}")
    public ChatMessage sendMessageToChannel(@DestinationVariable Long channelId, 
                                           Map<String, String> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }

        String username = payload.get("username");
        String content = payload.get("content");

        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            throw new IllegalArgumentException("Invalid username");
        }
        if (!ValidationUtils.isValidContent(content, maxMessageLength)) {
            throw new IllegalArgumentException("Invalid message content");
        }
        
        // Verify channel exists
        if (!channelService.getChannelById(channelId).isPresent()) {
            throw new IllegalArgumentException("Channel does not exist");
        }
        
        // Trim username and content before saving to ensure consistency
        String trimmedUsername = username.trim();
        String trimmedContent = content.trim();
        
        return chatService.saveMessage(trimmedUsername, trimmedContent, channelId);
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/messages")
    public ChatMessage userJoin(Map<String, String> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }
        
        String username = payload.get("username");
        
        // Legacy /chat.join sends to the global topic; always associate with the default channel
        Long channelId = channelService.getOrCreateDefaultChannel().getId();
        
        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            throw new IllegalArgumentException("The 'username' field must be valid");
        }
        
        // Trim username before using in system message
        String trimmedUsername = username.trim();
        return chatService.saveMessage("System", trimmedUsername + " has joined the chat", channelId);
    }

    @MessageMapping("/chat.join/{channelId}")
    @SendTo("/topic/messages/{channelId}")
    public ChatMessage userJoinChannel(@DestinationVariable Long channelId,
                                      Map<String, String> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }
        
        String username = payload.get("username");
        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            throw new IllegalArgumentException("The 'username' field must be valid");
        }
        
        // Verify channel exists
        if (!channelService.getChannelById(channelId).isPresent()) {
            throw new IllegalArgumentException("Channel does not exist");
        }
        
        // Trim username before using in system message
        String trimmedUsername = username.trim();
        return chatService.saveMessage("System", trimmedUsername + " has joined the chat", channelId);
    }

    @MessageMapping("/chat.typing/{channelId}")
    @SendTo("/topic/typing/{channelId}")
    public TypingIndicator userTyping(@DestinationVariable Long channelId,
                                     Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }
        
        String username = (String) payload.get("username");
        Boolean typing = (Boolean) payload.get("typing");
        
        if (!ValidationUtils.isValidUsername(username, minUsernameLength, maxUsernameLength)) {
            throw new IllegalArgumentException("Invalid username");
        }
        
        if (typing == null) {
            typing = true; // Default to typing=true if not specified
        }
        
        // Verify channel exists
        if (!channelService.getChannelById(channelId).isPresent()) {
            throw new IllegalArgumentException("Channel does not exist");
        }
        
        return new TypingIndicator(username.trim(), channelId, typing);
    }
}

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins}")
class MessageRestController {
    
    private static final int MAX_LIMIT = 500;

    @Autowired
    private ChatService chatService;

    @GetMapping("/api/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long channelId) {
        
        if (limit < 1) {
            limit = 1;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        
        List<ChatMessage> messages;
        if (channelId != null) {
            messages = chatService.getRecentMessagesByChannel(channelId, limit);
        } else {
            messages = chatService.getRecentMessages(limit);
        }
        return ResponseEntity.ok(messages);
    }
}
