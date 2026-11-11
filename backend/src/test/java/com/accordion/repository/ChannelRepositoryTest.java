package com.accordion.repository;

import com.accordion.model.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes the channel-name lookup ChannelService relies on (including for the
 * default "general" channel) and the database-level uniqueness constraint on names.
 * Runs against the embedded H2 database that @DataJpaTest provisions per test.
 */
@DataJpaTest
class ChannelRepositoryTest {

    @Autowired
    private ChannelRepository channelRepository;

    @Test
    void findByNameReturnsSavedChannel() {
        channelRepository.save(new Channel("general", "General discussion", "System"));

        Optional<Channel> result = channelRepository.findByName("general");

        assertTrue(result.isPresent());
        assertEquals("General discussion", result.get().getDescription());
        assertEquals("System", result.get().getCreatedBy());
        assertNotNull(result.get().getId());
    }

    @Test
    void findByNameReturnsEmptyForUnknownChannel() {
        channelRepository.save(new Channel("general", "General discussion", "System"));

        assertTrue(channelRepository.findByName("random").isEmpty());
    }

    @Test
    void findByNameIsCaseSensitive() {
        channelRepository.save(new Channel("general", "General discussion", "System"));

        assertTrue(channelRepository.findByName("General").isEmpty());
    }

    @Test
    void savingDuplicateChannelNameViolatesUniqueConstraint() {
        channelRepository.saveAndFlush(new Channel("general", "First", "alice"));

        assertThrows(DataIntegrityViolationException.class,
                () -> channelRepository.saveAndFlush(new Channel("general", "Second", "bob")));
    }
}
