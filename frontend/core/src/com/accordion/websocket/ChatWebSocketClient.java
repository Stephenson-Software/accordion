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
    private static final String CONNECT_FRAME = "CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\0";
    private static final String SUBSCRIBE_TEMPLATE = "SUBSCRIBE\nid:sub-0\ndestination:/topic/messages\n\n\0";
    private static final String SEND_TEMPLATE = "SEND\ndestination:/app/chat.send\ncontent-type:application/json\n\n%s\0";
    
    private final Gson gson = new Gson();
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private String username;
    private boolean connected = false;

    public interface MessageListener {
        void onMessage(String username, String content, String timestamp);
        void onConnectionStatusChanged(boolean connected);
    }

    public ChatWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.info("WebSocket connection opened");
        send(CONNECT_FRAME);
    }

    @Override
    public void onMessage(String message) {
        LOGGER.fine("Received: " + message);
        
        if (message.startsWith("CONNECTED")) {
            connected = true;
            notifyConnectionStatus(true);
            send(SUBSCRIBE_TEMPLATE);
            
            // Send join notification
            Map<String, String> joinPayload = new HashMap<>();
            joinPayload.put("username", username);
            String joinJson = gson.toJson(joinPayload);
            String joinFrame = String.format("SEND\ndestination:/app/chat.join\ncontent-type:application/json\n\n%s\0", joinJson);
            send(joinFrame);
        } else if (message.startsWith("MESSAGE")) {
            handleMessage(message);
        }
    }

    private void handleMessage(String stompMessage) {
        try {
            // Extract JSON payload from STOMP message
            String[] lines = stompMessage.split("\n\n", 2);
            if (lines.length > 1) {
                String jsonPayload = lines[1].replace("\0", "");
                JsonObject json = gson.fromJson(jsonPayload, JsonObject.class);
                
                String msgUsername = json.has("username") ? json.get("username").getAsString() : "Unknown";
                String content = json.has("content") ? json.get("content").getAsString() : "";
                String timestamp = json.has("timestamp") ? json.get("timestamp").getAsString() : "";
                
                for (MessageListener listener : listeners) {
                    listener.onMessage(msgUsername, content, timestamp);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing message", e);
        }
    }

    public void sendChatMessage(String content) {
        if (!connected) {
            LOGGER.warning("Cannot send message: Not connected");
            return;
        }
        
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("content", content);
        
        String json = gson.toJson(payload);
        String frame = String.format(SEND_TEMPLATE, json);
        
        send(frame);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("WebSocket connection closed: " + reason);
        connected = false;
        notifyConnectionStatus(false);
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.log(Level.SEVERE, "WebSocket error", ex);
        connected = false;
        notifyConnectionStatus(false);
    }

    private void notifyConnectionStatus(boolean status) {
        for (MessageListener listener : listeners) {
            listener.onConnectionStatusChanged(status);
        }
    }

    public boolean isConnected() {
        return connected && !isClosed();
    }
}
