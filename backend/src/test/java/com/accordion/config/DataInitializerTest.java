package com.accordion.config;

import com.accordion.model.Channel;
import com.accordion.service.ChannelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private ChannelService channelService;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void testRunEnsuresDefaultChannelExists() {
        when(channelService.getOrCreateDefaultChannel())
            .thenReturn(new Channel("general", "General discussion", "System"));

        dataInitializer.run();

        verify(channelService, times(1)).getOrCreateDefaultChannel();
        verifyNoMoreInteractions(channelService);
    }

    @Test
    void testRunIgnoresCommandLineArguments() {
        when(channelService.getOrCreateDefaultChannel())
            .thenReturn(new Channel("general", "General discussion", "System"));

        dataInitializer.run("--some.flag=value", "positional");

        verify(channelService, times(1)).getOrCreateDefaultChannel();
        verifyNoMoreInteractions(channelService);
    }

    @Test
    void testRunPropagatesChannelServiceFailure() {
        // A seeding failure is not swallowed, so it aborts application startup.
        when(channelService.getOrCreateDefaultChannel())
            .thenThrow(new IllegalStateException("database unavailable"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> dataInitializer.run());
        assertEquals("database unavailable", thrown.getMessage());
    }
}
