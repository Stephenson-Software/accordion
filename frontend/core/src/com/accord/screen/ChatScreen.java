package com.accord.screen;

import com.accord.AccordGame;
import com.accord.config.AppConfig;
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
import java.util.List;
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
    private List<String> messages;
    private static final int MAX_MESSAGES = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // For tracking consecutive duplicate messages
    private String lastMessageUsername = null;
    private String lastMessageContent = null;
    private int lastMessageCount = 1;

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
        // Format timestamp
        String timeStr = "";
        try {
            LocalDateTime dt = LocalDateTime.parse(timestamp);
            timeStr = dt.format(TIME_FORMATTER);
        } catch (Exception e) {
            // If timestamp parsing fails, log and use current time
            LOGGER.warning("Failed to parse timestamp: " + timestamp + ", error: " + e.getMessage());
            try {
                timeStr = LocalDateTime.now().format(TIME_FORMATTER);
            } catch (Exception ex) {
                timeStr = "??:??:??";
            }
        }

        // Check if this is a consecutive duplicate message (same user and same content)
        boolean isDuplicate = lastMessageUsername != null && 
                              lastMessageUsername.equals(msgUsername) && 
                              lastMessageContent != null && 
                              lastMessageContent.equals(content);
        
        if (isDuplicate && !messages.isEmpty()) {
            // Update the last message with incremented count
            lastMessageCount++;
            // Note: Format is " (x2)" for LibGDX, while webapp uses a styled badge.
            // This difference is intentional: LibGDX uses plain text labels,
            // while webapp can style a separate DOM element with colors/backgrounds.
            String countIndicator = " (x" + lastMessageCount + ")";
            String updatedMessage = String.format("[%s] %s: %s%s", timeStr, msgUsername, content, countIndicator);
            
            // Replace the last message
            messages.set(messages.size() - 1, updatedMessage);
        } else {
            // Add new message (not a duplicate, or different from last message)
            String formattedMessage = String.format("[%s] %s: %s", timeStr, msgUsername, content);
            messages.add(formattedMessage);
            
            // Update tracking variables to reference this newly added message
            // Note: These track content/username, not list indices
            lastMessageUsername = msgUsername;
            lastMessageContent = content;
            lastMessageCount = 1;
            
            // Keep only last MAX_MESSAGES
            if (messages.size() > MAX_MESSAGES) {
                // Remove the oldest message (index 0)
                // This is safe: our tracking variables now point to the message we JUST added
                // (which is at the end of the list), so removing the oldest doesn't affect tracking
                messages.remove(0);
            }
        }

        // Update UI
        messagesTable.clear();
        for (String msg : messages) {
            Label msgLabel = new Label(msg, skin);
            msgLabel.setWrap(true);
            msgLabel.setAlignment(Align.left);
            messagesTable.add(msgLabel).expandX().fillX().left().padBottom(5);
            messagesTable.row();
        }
        
        // Scroll to bottom
        scrollPane.layout();
        scrollPane.setScrollPercentY(1f);
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
