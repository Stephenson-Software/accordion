package com.accord.model;

/**
 * DTO for typing indicator events.
 * Not persisted to database - only used for real-time WebSocket messaging.
 */
public class TypingIndicator {
    private String username;
    private Long channelId;
    private boolean typing;

    public TypingIndicator() {
    }

    public TypingIndicator(String username, Long channelId, boolean typing) {
        this.username = username;
        this.channelId = channelId;
        this.typing = typing;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
