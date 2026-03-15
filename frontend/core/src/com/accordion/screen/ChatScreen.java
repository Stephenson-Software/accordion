package com.accordion.screen;

import com.accordion.AccordionGame;
import com.accordion.config.AppConfig;
import com.accordion.service.ApiClient;
import com.accordion.websocket.ChatWebSocketClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatScreen implements Screen {
    private static final Logger LOGGER = Logger.getLogger(ChatScreen.class.getName());
    
    private final AccordionGame game;
    private final String username;
    private Stage stage;
    private Skin skin;
    private TextField messageField;
    private TextButton sendButton;
    private ScrollPane scrollPane;
    private Table messagesTable;
    private Label statusLabel;
    private Label channelNameLabel;
    private Label typingLabel;
    private Table channelListTable;
    private ScrollPane channelScrollPane;
    private ChatWebSocketClient webSocketClient;
    private List<String> messages;
    private static final int MAX_MESSAGES = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // For tracking consecutive duplicate messages
    private String lastMessageUsername = null;
    private String lastMessageContent = null;
    private int lastMessageCount = 1;
    
    // Channel management
    private List<ApiClient.ChannelInfo> channels = new ArrayList<>();
    private long currentChannelId = -1;
    
    // Lifecycle guard for background threads
    private volatile boolean disposed = false;
    
    // Typing indicator tracking
    private final Map<String, Long> typingUsers = new HashMap<>();
    private boolean isCurrentlyTyping = false;
    private long lastTypingSentTime = 0;

    public ChatScreen(final AccordionGame game, String username) {
        this.game = game;
        this.username = username;
        this.messages = new ArrayList<>();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Main table with sidebar layout
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // === LEFT SIDEBAR: Channel list ===
        Table sidebarTable = new Table();
        sidebarTable.top();
        
        Label channelsHeader = new Label("Channels", skin);
        channelsHeader.setFontScale(1.2f);
        sidebarTable.add(channelsHeader).expandX().left().padLeft(5).padTop(5).padBottom(5);
        sidebarTable.row();
        
        // Channel list (scrollable)
        channelListTable = new Table();
        channelListTable.top().left();
        channelScrollPane = new ScrollPane(channelListTable, skin);
        channelScrollPane.setFadeScrollBars(false);
        channelScrollPane.setScrollingDisabled(true, false);
        sidebarTable.add(channelScrollPane).expand().fill().pad(2);
        sidebarTable.row();
        
        // New Channel button
        TextButton newChannelButton = new TextButton("+ New Channel", skin);
        newChannelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCreateChannelDialog();
            }
        });
        sidebarTable.add(newChannelButton).fillX().pad(5);
        sidebarTable.row();
        
        // Logout button
        TextButton logoutButton = new TextButton("Logout", skin);
        logoutButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleLogout();
            }
        });
        sidebarTable.add(logoutButton).fillX().pad(5).padBottom(5);

        // === RIGHT SIDE: Chat area ===
        Table chatTable = new Table();
        
        // Header row
        Table headerTable = new Table();
        channelNameLabel = new Label("# general", skin);
        channelNameLabel.setFontScale(1.3f);
        headerTable.add(channelNameLabel).expandX().left().padLeft(10);
        
        statusLabel = new Label("Connecting...", skin);
        statusLabel.setColor(Color.YELLOW);
        headerTable.add(statusLabel).right().padRight(10);
        
        chatTable.add(headerTable).fillX().padTop(5);
        chatTable.row();

        // Messages area
        messagesTable = new Table();
        messagesTable.top().left();
        scrollPane = new ScrollPane(messagesTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        chatTable.add(scrollPane).expand().fill().pad(5);
        chatTable.row();
        
        // Typing indicator
        typingLabel = new Label("", skin);
        typingLabel.setColor(Color.LIGHT_GRAY);
        chatTable.add(typingLabel).fillX().left().padLeft(10).height(20);
        chatTable.row();

        // Input area
        messageField = new TextField("", skin);
        messageField.setMessageText("Type your message...");
        messageField.setMaxLength(AppConfig.MAX_MESSAGE_LENGTH);
        
        // Typing indicator on key input
        messageField.addListener(new InputListener() {
            @Override
            public boolean keyTyped(InputEvent event, char character) {
                onMessageInputChanged();
                return false;
            }
        });
        
        sendButton = new TextButton("Send", skin);
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendMessage();
            }
        });

        Table inputTable = new Table();
        inputTable.add(messageField).expandX().fillX().padRight(5);
        inputTable.add(sendButton).width(80);
        chatTable.add(inputTable).fillX().pad(5);

        // Assemble main layout: sidebar | chat
        mainTable.add(sidebarTable).width(180).fillY().padRight(2);
        mainTable.add(chatTable).expand().fill();

        // Load channels and connect WebSocket
        loadChannels();
        connectWebSocket();
    }

    private void loadChannels() {
        new Thread(() -> {
            try {
                List<ApiClient.ChannelInfo> loadedChannels = game.apiClient.getChannels();
                Gdx.app.postRunnable(() -> {
                    if (disposed) return;
                    channels = loadedChannels;
                    displayChannels();
                    
                    // Auto-select first channel if none selected
                    if (currentChannelId == -1 && !channels.isEmpty()) {
                        switchChannel(channels.get(0).id, channels.get(0).name);
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load channels", e);
                Gdx.app.postRunnable(() -> {
                    if (disposed) return;
                    addMessage("System", "Failed to load channels: " + e.getMessage(),
                              LocalDateTime.now().toString());
                });
            }
        }).start();
    }

    private void displayChannels() {
        channelListTable.clear();
        for (final ApiClient.ChannelInfo channel : channels) {
            TextButton channelBtn = new TextButton("# " + channel.name, skin);
            if (channel.id == currentChannelId) {
                channelBtn.setColor(Color.CYAN);
            }
            channelBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    switchChannel(channel.id, channel.name);
                }
            });
            channelListTable.add(channelBtn).fillX().padBottom(2);
            channelListTable.row();
        }
    }

    private void switchChannel(long channelId, String channelName) {
        if (channelId == currentChannelId) return;
        
        currentChannelId = channelId;
        channelNameLabel.setText("# " + channelName);
        
        // Clear messages and typing state
        messages.clear();
        lastMessageUsername = null;
        lastMessageContent = null;
        lastMessageCount = 1;
        typingUsers.clear();
        updateTypingDisplay();
        isCurrentlyTyping = false;
        
        // Update channel list highlighting
        displayChannels();
        
        // Subscribe to new channel via WebSocket
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.subscribeToChannel(channelId);
        }
        
        // Load message history
        loadMessageHistory(channelId);
        
        // Refresh messages UI
        refreshMessagesUI();
    }

    private void loadMessageHistory(long channelId) {
        new Thread(() -> {
            try {
                List<ApiClient.MessageInfo> history = game.apiClient.getMessages(channelId, 50);
                Gdx.app.postRunnable(() -> {
                    if (disposed) return;
                    // Only apply if still on the same channel
                    if (channelId != currentChannelId) {
                        LOGGER.fine("Discarding message history for channel " + channelId 
                            + " (current channel is " + currentChannelId + ")");
                        return;
                    }
                    
                    // Batch insert all history messages without refreshing UI each time
                    for (ApiClient.MessageInfo msg : history) {
                        addMessageToList(msg.username, msg.content, msg.timestamp);
                    }
                    refreshMessagesUI();
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load message history", e);
                Gdx.app.postRunnable(() -> {
                    if (disposed) return;
                    addMessage("System", "Failed to load message history: " + e.getMessage(),
                              LocalDateTime.now().toString());
                });
            }
        }).start();
    }

    private void showCreateChannelDialog() {
        final Dialog dialog = new Dialog("Create Channel", skin);
        
        final TextField nameField = new TextField("", skin);
        nameField.setMessageText("Channel name");
        
        final TextField descField = new TextField("", skin);
        descField.setMessageText("Description (optional)");
        
        final Label dialogError = new Label("", skin);
        dialogError.setColor(Color.RED);
        dialogError.setWrap(true);
        
        dialog.getContentTable().add(new Label("Name:", skin)).padRight(5);
        dialog.getContentTable().add(nameField).width(250);
        dialog.getContentTable().row().padTop(5);
        dialog.getContentTable().add(new Label("Description:", skin)).padRight(5);
        dialog.getContentTable().add(descField).width(250);
        dialog.getContentTable().row().padTop(5);
        dialog.getContentTable().add(dialogError).colspan(2).width(300);
        
        TextButton createBtn = new TextButton("Create", skin);
        TextButton cancelBtn = new TextButton("Cancel", skin);
        
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = nameField.getText().trim();
                String description = descField.getText().trim();
                
                if (name.length() < AppConfig.MIN_CHANNEL_NAME_LENGTH || 
                    name.length() > AppConfig.MAX_CHANNEL_NAME_LENGTH) {
                    dialogError.setText("Name must be " + AppConfig.MIN_CHANNEL_NAME_LENGTH + 
                        "-" + AppConfig.MAX_CHANNEL_NAME_LENGTH + " characters");
                    return;
                }
                if (!name.matches(AppConfig.CHANNEL_NAME_PATTERN)) {
                    dialogError.setText("Name: letters, numbers, hyphens, underscores only");
                    return;
                }
                if (description.length() > AppConfig.MAX_CHANNEL_DESCRIPTION_LENGTH) {
                    dialogError.setText("Description too long (max " + 
                        AppConfig.MAX_CHANNEL_DESCRIPTION_LENGTH + ")");
                    return;
                }
                
                createBtn.setDisabled(true);
                createChannel(name, description, dialog);
            }
        });
        
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        
        dialog.button(createBtn);
        dialog.button(cancelBtn);
        dialog.show(stage);
    }

    private void createChannel(final String name, final String description, final Dialog dialog) {
        new Thread(() -> {
            try {
                ApiClient.ChannelInfo newChannel = game.apiClient.createChannel(name, description, username);
                Gdx.app.postRunnable(() -> {
                    if (disposed) return;
                    dialog.hide();
                    loadChannels();
                    switchChannel(newChannel.id, newChannel.name);
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to create channel", e);
                Gdx.app.postRunnable(() -> {
                    if (disposed) return;
                    addMessage("System", "Failed to create channel: " + e.getMessage(),
                              LocalDateTime.now().toString());
                    dialog.hide();
                });
            }
        }).start();
    }

    private void handleLogout() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        game.apiClient.setJwtToken(null);
        game.username = null;
        game.setScreen(new LoginScreen(game));
    }

    private void connectWebSocket() {
        try {
            String wsUrl = AppConfig.getWebSocketUrl();
            LOGGER.info("Connecting to WebSocket at: " + wsUrl);
            
            URI uri = new URI(wsUrl);
            webSocketClient = new ChatWebSocketClient(uri);
            webSocketClient.setUsername(username);
            webSocketClient.setJwtToken(game.apiClient.getJwtToken());
            
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
                            // Subscribe to current channel once connected
                            if (currentChannelId != -1) {
                                webSocketClient.subscribeToChannel(currentChannelId);
                            }
                        } else {
                            statusLabel.setText("Disconnected");
                            statusLabel.setColor(Color.RED);
                        }
                    });
                }
            });
            
            webSocketClient.addTypingListener(new ChatWebSocketClient.TypingListener() {
                @Override
                public void onTypingIndicator(String typingUsername, boolean typing) {
                    Gdx.app.postRunnable(() -> {
                        if (typing) {
                            typingUsers.put(typingUsername, System.currentTimeMillis());
                        } else {
                            typingUsers.remove(typingUsername);
                        }
                        updateTypingDisplay();
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

    private void onMessageInputChanged() {
        String text = messageField.getText().trim();
        long now = System.currentTimeMillis();
        
        if (!text.isEmpty()) {
            if (!isCurrentlyTyping || (now - lastTypingSentTime > AppConfig.TYPING_DEBOUNCE_MS)) {
                isCurrentlyTyping = true;
                lastTypingSentTime = now;
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    webSocketClient.sendTypingIndicator(true);
                }
            }
        } else {
            if (isCurrentlyTyping) {
                isCurrentlyTyping = false;
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    webSocketClient.sendTypingIndicator(false);
                }
            }
        }
    }

    private void updateTypingDisplay() {
        if (typingUsers.isEmpty()) {
            typingLabel.setText("");
            return;
        }
        
        List<String> names = new ArrayList<>(typingUsers.keySet());
        StringBuilder sb = new StringBuilder();
        if (names.size() == 1) {
            sb.append(names.get(0)).append(" is typing...");
        } else if (names.size() == 2) {
            sb.append(names.get(0)).append(" and ").append(names.get(1)).append(" are typing...");
        } else {
            sb.append(names.size()).append(" people are typing...");
        }
        typingLabel.setText(sb.toString());
    }

    /**
     * Add a message to the internal list without refreshing the UI.
     * Used for batch operations like loading message history.
     */
    private void addMessageToList(String msgUsername, String content, String timestamp) {
        // Format timestamp
        String timeStr = "";
        try {
            LocalDateTime dt = LocalDateTime.parse(timestamp);
            timeStr = dt.format(TIME_FORMATTER);
        } catch (Exception e) {
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
            lastMessageCount++;
            String countIndicator = " (x" + lastMessageCount + ")";
            String updatedMessage = String.format("[%s] %s: %s%s", timeStr, msgUsername, content, countIndicator);
            messages.set(messages.size() - 1, updatedMessage);
        } else {
            String formattedMessage = String.format("[%s] %s: %s", timeStr, msgUsername, content);
            messages.add(formattedMessage);
            lastMessageUsername = msgUsername;
            lastMessageContent = content;
            lastMessageCount = 1;
            
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }
    }

    private void addMessage(String msgUsername, String content, String timestamp) {
        addMessageToList(msgUsername, content, timestamp);
        refreshMessagesUI();
    }

    private void refreshMessagesUI() {
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
            
            // Clear typing indicator when message sent
            isCurrentlyTyping = false;
            webSocketClient.sendTypingIndicator(false);
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
        
        // Expire typing indicators after timeout
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Map.Entry<String, Long>> it = typingUsers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > AppConfig.TYPING_TIMEOUT_MS) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            updateTypingDisplay();
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
        disposed = true;
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        stage.dispose();
        skin.dispose();
    }
}
