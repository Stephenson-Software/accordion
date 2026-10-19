package com.accordion.config;

import com.accordion.security.WebSocketAuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    private WebSocketConfig config;
    private WebSocketAuthInterceptor interceptor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        config = new WebSocketConfig();
        interceptor = mock(WebSocketAuthInterceptor.class);
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(config, "webSocketAuthInterceptor", interceptor);
        ReflectionTestUtils.setField(config, "objectMapper", objectMapper);
    }

    private StompWebSocketEndpointRegistration registerWithOrigins(String allowedOrigins) {
        ReflectionTestUtils.setField(config, "allowedOrigins", allowedOrigins);
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint(any(String[].class))).thenReturn(registration);
        when(registration.setAllowedOriginPatterns(any(String[].class))).thenReturn(registration);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws");
        return registration;
    }

    @Test
    void testRegisterStompEndpoints_WildcardOrigin() {
        StompWebSocketEndpointRegistration registration = registerWithOrigins("*");

        verify(registration).setAllowedOriginPatterns("*");
        verify(registration).withSockJS();
    }

    @Test
    void testRegisterStompEndpoints_SplitsCommaSeparatedOrigins() {
        StompWebSocketEndpointRegistration registration =
            registerWithOrigins("http://localhost:3000,https://chat.example.com");

        verify(registration).setAllowedOriginPatterns("http://localhost:3000", "https://chat.example.com");
    }

    @Test
    void testRegisterStompEndpoints_TrimsWhitespaceAroundOrigins() {
        StompWebSocketEndpointRegistration registration =
            registerWithOrigins("  http://localhost:3000 , https://chat.example.com  ");

        verify(registration).setAllowedOriginPatterns("http://localhost:3000", "https://chat.example.com");
    }

    @Test
    void testConfigureMessageBroker_UsesTopicBrokerAndAppPrefix() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void testConfigureClientInboundChannel_RegistersAuthInterceptor() {
        ChannelRegistration registration = mock(ChannelRegistration.class);

        config.configureClientInboundChannel(registration);

        verify(registration).interceptors(interceptor);
    }

    @Test
    void testConfigureMessageConverters_AddsJacksonConverterWithSharedObjectMapper() {
        List<MessageConverter> converters = new ArrayList<>();

        boolean addDefaults = config.configureMessageConverters(converters);

        assertFalse(addDefaults, "Default converters should not be added alongside the Jackson converter");
        assertEquals(1, converters.size());
        MappingJackson2MessageConverter converter =
            assertInstanceOf(MappingJackson2MessageConverter.class, converters.get(0));
        assertSame(objectMapper, converter.getObjectMapper());
    }
}
