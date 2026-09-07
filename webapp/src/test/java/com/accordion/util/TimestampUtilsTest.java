package com.accordion.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test class for timestamp-related utilities
 */
class TimestampUtilsTest {

    @Test
    void testLocalDateTimeSerializationFormat() {
        // Test that LocalDateTime can be properly serialized and parsed
        LocalDateTime now = LocalDateTime.now();
        String serialized = now.toString();
        
        // Should be able to parse it back
        LocalDateTime parsed = LocalDateTime.parse(serialized);
        assertEquals(now, parsed, "LocalDateTime should serialize and deserialize correctly");
    }

    @Test
    void testTimestampFormatting() {
        // Test specific timestamp formatting
        LocalDateTime testTime = LocalDateTime.of(2026, 2, 15, 14, 30, 45);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        String formatted = testTime.format(formatter);
        assertEquals("14:30:45", formatted, "Timestamp should format correctly");
    }

    @Test
    void testDateComparison() {
        // Test date comparison for date separators
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime sameDay = today.minusHours(2);
        
        // Same day should have same date string
        assertEquals(today.toLocalDate(), sameDay.toLocalDate(), 
            "Timestamps from same day should have same date");
        
        // Different days should have different date strings
        assertNotEquals(today.toLocalDate(), yesterday.toLocalDate(), 
            "Timestamps from different days should have different dates");
    }

    @Test
    void testRelativeTimeCalculation() {
        LocalDateTime now = LocalDateTime.now();
        
        // Test various time differences
        LocalDateTime fiveSecondsAgo = now.minusSeconds(5);
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime oneDayAgo = now.minusDays(1);
        
        // Verify time differences are correct
        assertTrue(java.time.Duration.between(fiveSecondsAgo, now).getSeconds() == 5,
            "Five seconds difference should be calculated correctly");
        assertTrue(java.time.Duration.between(fiveMinutesAgo, now).toMinutes() == 5,
            "Five minutes difference should be calculated correctly");
        assertTrue(java.time.Duration.between(oneHourAgo, now).toHours() == 1,
            "One hour difference should be calculated correctly");
        assertTrue(java.time.Duration.between(oneDayAgo, now).toDays() == 1,
            "One day difference should be calculated correctly");
    }

    @Test
    void testLocalDateTimeNow() {
        // Ensure LocalDateTime.now() works correctly
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime after = LocalDateTime.now();
        
        // After should be equal or later than before
        assertFalse(after.isBefore(before), 
            "Later timestamp should not be before earlier timestamp");
    }

    @Test
    void testIso8601FormatLacksTimezone() {
        // Verify that LocalDateTime.toString() produces ISO-8601 without timezone suffix.
        // This is why JavaScript must treat the value as UTC (by appending 'Z').
        LocalDateTime dt = LocalDateTime.of(2026, 2, 15, 10, 0, 0);
        String serialized = dt.toString();
        assertFalse(serialized.endsWith("Z"),
            "LocalDateTime serialization should not include a timezone suffix");
        assertFalse(serialized.contains("+"),
            "LocalDateTime serialization should not include a UTC offset");
        assertTrue(serialized.startsWith("2026-02-15T10:00"),
            "LocalDateTime serialization should use ISO-8601 date-time format");
    }

    @Test
    void testTimestampSubtraction_SameZoneGivesCorrectDiff() {
        // Simulate the server producing a timestamp 5 minutes ago, and the
        // client computing the diff without timezone confusion.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);

        long diffSeconds = java.time.Duration.between(fiveMinutesAgo, now).getSeconds();
        assertEquals(300, diffSeconds,
            "Difference between now and 5 minutes ago should be 300 seconds");
    }
}
