package com.accordion.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // STOMP is the protocol's synonym for CONNECT; Spring opens a session for either
        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            
            if (authToken == null || !authToken.startsWith("Bearer ")) {
                logger.warn("WebSocket CONNECT rejected: missing or invalid Authorization header");
                // Reject unauthenticated CONNECT by returning null
                return null;
            }

            String token = authToken.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (!jwtUtil.validateToken(token, userDetails.getUsername())) {
                    logger.warn("WebSocket CONNECT rejected: JWT validation failed for user '{}'", username);
                    return null;
                }

                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                accessor.setUser(authentication);
                
                logger.debug("WebSocket CONNECT authenticated for user '{}'", username);
            } catch (Exception e) {
                // Authentication failed - reject the WebSocket connection
                logger.warn("WebSocket CONNECT rejected: Authentication failed - {}", e.getMessage());
                return null;
            }
        } else if ((StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command))
                && accessor.getUser() == null) {
            // Spring does not require a CONNECT before these frames, and it keeps delivering
            // them after a rejected one, so the session's user is the only reliable check
            logger.warn("WebSocket {} rejected: session is not authenticated", command);
            return null;
        }
        
        return message;
    }
}
