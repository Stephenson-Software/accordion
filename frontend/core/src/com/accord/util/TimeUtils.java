package com.accord.util;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for formatting timestamps with relative time display
 */
public class TimeUtils {
    
    private static final DateTimeFormatter FULL_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    /**
     * Get relative time string (e.g., "Just now", "5 minutes ago")
     */
    public static String getRelativeTime(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(timestamp, now);
        
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (seconds < 10) {
            return "Just now";
        } else if (seconds < 60) {
            return seconds + " seconds ago";
        } else if (minutes == 1) {
            return "1 minute ago";
        } else if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours == 1) {
            return "1 hour ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else if (days == 1) {
            return "Yesterday";
        } else if (days < 7) {
            return days + " days ago";
        } else {
            return timestamp.format(DATE_FORMATTER);
        }
    }
    
    /**
     * Get full timestamp string for tooltips/detailed view
     */
    public static String getFullTimestamp(LocalDateTime timestamp) {
        return timestamp.format(FULL_FORMATTER);
    }
    
    /**
     * Get date separator text (e.g., "Today", "Yesterday", "February 12, 2026")
     */
    public static String getDateSeparator(LocalDateTime timestamp) {
        LocalDate messageDate = timestamp.toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        if (messageDate.equals(today)) {
            return "Today";
        } else if (messageDate.equals(yesterday)) {
            return "Yesterday";
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        }
    }
    
    /**
     * Check if a date separator should be shown between two messages
     */
    public static boolean shouldShowDateSeparator(LocalDateTime current, LocalDateTime previous) {
        if (previous == null) {
            return true;
        }
        
        return !current.toLocalDate().equals(previous.toLocalDate());
    }
}
