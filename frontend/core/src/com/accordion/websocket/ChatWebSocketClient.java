package com.accordion.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatWebSocketClient extends WebSocketClient {
    private static final Logger LOGGER = Logger.getLogger(ChatWebSocketClient.class.getName());
    
    private final Gson gson = new Gson();
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TypingListener> typingListeners = new CopyOnWriteArrayList<>();
    private String username;
    private String jwtToken;
    private boolean connected = false;
    
    // Current channel subscription tracking
    private Long currentChannelId;
    private int subscriptionCounter = 0;
    private String messageSubId;
    private String typingSubId;

    public interface MessageListener {
        void onMessage(String username, String content, String timestamp);
        void onConnectionStatusChanged(boolean connected);
    }

    public interface TypingListener {
        void onTypingIndicator(String username, boolean typing);
    }

    public ChatWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public void addTypingListener(TypingListener listener) {
        typingListeners.add(listener);
    }

    public void removeTypingListener(TypingListener listener) {
        typingListeners.remove(listener);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.info("WebSocket connection opened");
        // Send STOMP CONNECT with JWT authorization
        StringBuilder connectFrame = new StringBuilder();
        connectFrame.append("CONNECT\n");
        connectFrame.append("accept-version:1.1,1.0\n");
        connectFrame.append("heart-beat:10000,10000\n");
        if (jwtToken != null && !jwtToken.isEmpty()) {
            connectFrame.append("Authorization:Bearer ").append(jwtToken).append("\n");
        }
        connectFrame.append("\n\0");
        send(connectFrame.toString());
    }

    @Override
    public void onMessage(String message) {
        LOGGER.fine("Received: " + message);
        
        if (message.startsWith("CONNECTED")) {
            connected = true;
            notifyConnectionStatus(true);
        } else if (message.startsWith("MESSAGE")) {
            handleMessage(message);
        } else if (message.startsWith("ERROR")) {
            LOGGER.warning("STOMP ERROR: " + message);
        }
    }

    /**
     * Subscribe to a specific channel's messages and typing indicators.
     * Unsubscribes from the previous channel if any.
     */
    public void subscribeToChannel(long channelId) {
        // Unsubscribe from previous channel
        if (messageSubId != null) {
            String unsubFrame = "UNSUBSCRIBE\nid:" + messageSubId + "\n\n\0";
            send(unsubFrame);
        }
        if (typingSubId != null) {
            String unsubFrame = "UNSUBSCRIBE\nid:" + typingSubId + "\n\n\0";
            send(unsubFrame);
        }

        currentChannelId = channelId;
        
        // Subscribe to channel messages
        messageSubId = "sub-" + (subscriptionCounter++);
        String msgSubFrame = "SUBSCRIBE\nid:" + messageSubId + 
            "\ndestination:/topic/messages/" + channelId + "\n\n\0";
        send(msgSubFrame);
        
        // Subscribe to channel typing indicators
        typingSubId = "sub-" + (subscriptionCounter++);
        String typingSubFrame = "SUBSCRIBE\nid:" + typingSubId + 
            "\ndestination:/topic/typing/" + channelId + "\n\n\0";
        send(typingSubFrame);
        
        // Send join notification
        Map<String, String> joinPayload = new HashMap<>();
        joinPayload.put("username", username);
        String joinJson = gson.toJson(joinPayload);
        String joinFrame = "SEND\ndestination:/app/chat.join/" + channelId + 
            "\ncontent-type:application/json\n\n" + joinJson + "\0";
        send(joinFrame);
    }

    private void handleMessage(String stompMessage) {
        try {
            // Parse STOMP headers to determine destination
            String[] parts = stompMessage.split("\n\n", 2);
            if (parts.length <= 1) {
                LOGGER.warning("Malformed STOMP message: missing payload separator");
                return;
            }
            
            String headers = parts[0];
            String jsonPayload = parts[1].replace("\0", "");
            
            // Check if this is a typing indicator message
            if (headers.contains("destination:/topic/typing/")) {
                handleTypingMessage(jsonPayload);
                return;
            }
            
            // Handle chat message
            JsonObject json = gson.fromJson(jsonPayload, JsonObject.class);
            String msgUsername = json.has("username") ? json.get("username").getAsString() : "Unknown";
            String content = json.has("content") ? json.get("content").getAsString() : "";
            String timestamp = json.has("timestamp") ? json.get("timestamp").getAsString() : "";
            
            for (MessageListener listener : listeners) {
                listener.onMessage(msgUsername, content, timestamp);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing message", e);
        }
    }

    private void handleTypingMessage(String jsonPayload) {
        try {
            JsonObject json = gson.fromJson(jsonPayload, JsonObject.class);
            String typingUsername = json.has("username") ? json.get("username").getAsString() : "";
            boolean typing = json.has("typing") && json.get("typing").getAsBoolean();
            
            // Don't notify about own typing
            if (typingUsername.equals(username)) return;
            
            for (TypingListener listener : typingListeners) {
                listener.onTypingIndicator(typingUsername, typing);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing typing indicator", e);
        }
    }

    public void sendChatMessage(String content) {
        if (!connected || currentChannelId == null) {
            LOGGER.warning("Cannot send message: Not connected or no channel selected");
            return;
        }
        
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("content", content);
        
        String json = gson.toJson(payload);
        String frame = "SEND\ndestination:/app/chat.send/" + currentChannelId + 
            "\ncontent-type:application/json\n\n" + json + "\0";
        send(frame);
    }

    /**
     * Send a typing indicator to the current channel.
     */
    public void sendTypingIndicator(boolean typing) {
        if (!connected || currentChannelId == null) return;
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("typing", typing);
        
        String json = gson.toJson(payload);
        String frame = "SEND\ndestination:/app/chat.typing/" + currentChannelId + 
            "\ncontent-type:application/json\n\n" + json + "\0";
        send(frame);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("WebSocket connection closed: " + reason);
        connected = false;
        resetSubscriptionState();
        notifyConnectionStatus(false);
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.log(Level.SEVERE, "WebSocket error", ex);
        connected = false;
        resetSubscriptionState();
        notifyConnectionStatus(false);
    }

    private void resetSubscriptionState() {
        messageSubId = null;
        typingSubId = null;
        subscriptionCounter = 0;
        currentChannelId = null;
    }

    private void notifyConnectionStatus(boolean status) {
        for (MessageListener listener : listeners) {
            listener.onConnectionStatusChanged(status);
        }
    }

    public boolean isConnected() {
        return connected && !isClosed();
    }

    public Long getCurrentChannelId() {
        return currentChannelId;
    }
}
