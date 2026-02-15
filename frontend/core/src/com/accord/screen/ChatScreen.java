package com.accord.screen;

import com.accord.AccordGame;
import com.accord.config.AppConfig;
import com.accord.util.TimeUtils;
import com.accord.websocket.ChatWebSocketClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ChatScreen implements Screen {
    private static final Logger LOGGER = Logger.getLogger(ChatScreen.class.getName());
    
    private final AccordGame game;
    private final String username;
    private Stage stage;
    private Skin skin;
    private TextField messageField;
    private TextButton sendButton;
    private ScrollPane scrollPane;
    private Table messagesTable;
    private Label statusLabel;
    private ChatWebSocketClient webSocketClient;
    private List<MessageEntry> messages;
    private static final int MAX_MESSAGES = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // For tracking consecutive duplicate messages
    private String lastMessageUsername = null;
    private String lastMessageContent = null;
    private int lastMessageCount = 1;
    private LocalDateTime lastMessageTimestamp = null;
    
    // For periodic timestamp updates
    private float timeSinceLastUpdate = 0f;
    private static final float UPDATE_INTERVAL = 30f; // Update every 30 seconds

    // Inner class to store message data
    private static class MessageEntry {
        String username;
        String content;
        LocalDateTime timestamp;
        int count;
        
        MessageEntry(String username, String content, LocalDateTime timestamp, int count) {
            this.username = username;
            this.content = content;
            this.timestamp = timestamp;
            this.count = count;
        }
    }

    public ChatScreen(final AccordGame game, String username) {
        this.game = game;
        this.username = username;
        this.messages = new ArrayList<>();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Main table
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Header
        Label titleLabel = new Label("Accord Chat - " + username, skin);
        titleLabel.setFontScale(1.5f);
        
        statusLabel = new Label("Connecting...", skin);
        statusLabel.setColor(Color.YELLOW);

        // Messages area
        messagesTable = new Table();
        messagesTable.top().left();
        scrollPane = new ScrollPane(messagesTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // Input area
        messageField = new TextField("", skin);
        messageField.setMessageText("Type your message...");
        messageField.setMaxLength(AppConfig.MAX_MESSAGE_LENGTH);
        
        sendButton = new TextButton("Send", skin);
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendMessage();
            }
        });

        // Layout
        mainTable.add(titleLabel).expandX().left().padLeft(10).padTop(10);
        mainTable.add(statusLabel).right().padRight(10).padTop(10);
        mainTable.row();
        mainTable.add(scrollPane).colspan(2).expand().fill().pad(10);
        mainTable.row();
        
        Table inputTable = new Table();
        inputTable.add(messageField).expandX().fillX().padRight(10);
        inputTable.add(sendButton).width(100);
        
        mainTable.add(inputTable).colspan(2).fillX().pad(10);

        // Connect to WebSocket
        connectWebSocket();
    }

    private void connectWebSocket() {
        try {
            String wsUrl = AppConfig.getWebSocketUrl();
            LOGGER.info("Connecting to WebSocket at: " + wsUrl);
            
            URI uri = new URI(wsUrl);
            webSocketClient = new ChatWebSocketClient(uri);
            webSocketClient.setUsername(username);
            
            webSocketClient.addMessageListener(new ChatWebSocketClient.MessageListener() {
                @Override
                public void onMessage(String msgUsername, String content, String timestamp) {
                    Gdx.app.postRunnable(() -> addMessage(msgUsername, content, timestamp));
                }

                @Override
                public void onConnectionStatusChanged(boolean connected) {
                    Gdx.app.postRunnable(() -> {
                        if (connected) {
                            statusLabel.setText("Connected");
                            statusLabel.setColor(Color.GREEN);
                        } else {
                            statusLabel.setText("Disconnected");
                            statusLabel.setColor(Color.RED);
                        }
                    });
                }
            });
            
            webSocketClient.connect();
        } catch (Exception e) {
            String errorMsg = "Failed to connect to WebSocket: " + e.getMessage();
            LOGGER.severe(errorMsg);
            Gdx.app.postRunnable(() -> {
                statusLabel.setText("Connection Failed");
                statusLabel.setColor(Color.RED);
                addMessage("System", "Failed to connect to server. Please check your connection.", 
                          LocalDateTime.now().toString());
            });
        }
    }

    private void addMessage(String msgUsername, String content, String timestamp) {
        // Parse timestamp
        LocalDateTime dt;
        try {
            dt = LocalDateTime.parse(timestamp);
        } catch (Exception e) {
            // If timestamp parsing fails, log and use current time
            LOGGER.warning("Failed to parse timestamp: " + timestamp + ", error: " + e.getMessage());
            dt = LocalDateTime.now();
        }

        // Check if this is a consecutive duplicate message (same user and same content)
        boolean isDuplicate = lastMessageUsername != null && 
                              lastMessageUsername.equals(msgUsername) && 
                              lastMessageContent != null && 
                              lastMessageContent.equals(content);
        
        if (isDuplicate && !messages.isEmpty()) {
            // Update the last message with incremented count
            lastMessageCount++;
            MessageEntry lastEntry = messages.get(messages.size() - 1);
            lastEntry.count = lastMessageCount;
            lastEntry.timestamp = dt; // Update to latest timestamp
            lastMessageTimestamp = dt;
        } else {
            // Add new message (not a duplicate, or different from last message)
            MessageEntry entry = new MessageEntry(msgUsername, content, dt, 1);
            messages.add(entry);
            
            // Update tracking variables
            lastMessageUsername = msgUsername;
            lastMessageContent = content;
            lastMessageCount = 1;
            lastMessageTimestamp = dt;
            
            // Keep only last MAX_MESSAGES
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }
        
        // Refresh the display
        refreshMessagesDisplay();
    }
    
    private void refreshMessagesDisplay() {
        messagesTable.clear();
        
        LocalDateTime previousTimestamp = null;
        
        for (MessageEntry entry : messages) {
            // Add date separator if needed
            if (TimeUtils.shouldShowDateSeparator(entry.timestamp, previousTimestamp)) {
                Label separatorLabel = new Label("--- " + TimeUtils.getDateSeparator(entry.timestamp) + " ---", skin);
                separatorLabel.setColor(Color.GRAY);
                separatorLabel.setAlignment(Align.center);
                messagesTable.add(separatorLabel).fillX().padTop(10).padBottom(5);
                messagesTable.row();
            }
            
            // Format message with relative time
            String relativeTime = TimeUtils.getRelativeTime(entry.timestamp);
            String countIndicator = entry.count > 1 ? " (x" + entry.count + ")" : "";
            String formattedMessage = String.format("[%s] %s: %s%s", relativeTime, entry.username, entry.content, countIndicator);
            
            Label messageLabel = new Label(formattedMessage, skin);
            messageLabel.setWrap(true);
            messageLabel.setAlignment(Align.left);
            messagesTable.add(messageLabel).fillX().expandX().padBottom(5);
            messagesTable.row();
            
            previousTimestamp = entry.timestamp;
        }
        
        // Scroll to bottom
        scrollPane.layout();
        scrollPane.setScrollPercentY(1.0f);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        
        if (message.isEmpty()) {
            return;
        }
        
        if (message.length() > AppConfig.MAX_MESSAGE_LENGTH) {
            addMessage("System", "Message too long. Maximum " + AppConfig.MAX_MESSAGE_LENGTH + " characters.", 
                      LocalDateTime.now().toString());
            return;
        }
        
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.sendChatMessage(message);
            messageField.setText("");
        } else {
            statusLabel.setText("Not connected");
            statusLabel.setColor(Color.RED);
            addMessage("System", "Not connected to server. Attempting to reconnect...", 
                      LocalDateTime.now().toString());
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update relative timestamps periodically
        timeSinceLastUpdate += delta;
        if (timeSinceLastUpdate >= UPDATE_INTERVAL) {
            timeSinceLastUpdate = 0f;
            if (!messages.isEmpty()) {
                refreshMessagesDisplay();
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        stage.dispose();
        skin.dispose();
    }
}
