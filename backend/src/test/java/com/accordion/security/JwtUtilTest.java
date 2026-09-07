package com.accordion.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String VALID_SECRET = "TestSecretKeyForJwtUtilTestsMinimum32BytesRequired";
    private static final long EXPIRATION_MS = 86400000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_MS);
    }

    @Test
    void testValidateSecret_NullSecretRejected() {
        ReflectionTestUtils.setField(jwtUtil, "secret", null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateSecret());
        assertTrue(exception.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void testValidateSecret_BlankSecretRejected() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "   ");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateSecret());
        assertTrue(exception.getMessage().contains("non-blank"));
    }

    @Test
    void testValidateSecret_TooShortSecretRejected() {
        String tooShort = "a".repeat(JwtUtil.MIN_SECRET_LENGTH_BYTES - 1);
        ReflectionTestUtils.setField(jwtUtil, "secret", tooShort);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateSecret());
        assertTrue(exception.getMessage().contains("at least 32 bytes"));
    }

    @Test
    void testValidateSecret_MinimumLengthSecretAccepted() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "a".repeat(JwtUtil.MIN_SECRET_LENGTH_BYTES));

        assertDoesNotThrow(() -> jwtUtil.validateSecret());
    }

    @Test
    void testValidateSecret_LengthMeasuredInBytesNotCharacters() {
        // 31 characters, but each '€' is 3 bytes in UTF-8, so the secret is 93 bytes
        String multiByteSecret = "€".repeat(31);
        ReflectionTestUtils.setField(jwtUtil, "secret", multiByteSecret);

        assertDoesNotThrow(() -> jwtUtil.validateSecret());
    }

    @Test
    void testApplicationPropertiesDeclaresJwtSecretWithoutDefault() throws IOException {
        // Regression guard: the ${JWT_SECRET:?...} form was read by Spring as a placeholder
        // with the literal default "?JWT_SECRET environment variable must be set", which is
        // long enough for HS256 and so silently signed tokens with a committed secret.
        // Any default at all reintroduces that, so the placeholder must stay bare.
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(input, "application.properties must be on the classpath");
            properties.load(input);
        }

        assertEquals("${JWT_SECRET}", properties.getProperty("jwt.secret"));
    }

    @Test
    void testGenerateToken_RoundTripsUsername() {
        String token = jwtUtil.generateToken("alice");

        assertNotNull(token);
        assertEquals("alice", jwtUtil.extractUsername(token));
    }

    @Test
    void testValidateToken_AcceptsMatchingUsername() {
        String token = jwtUtil.generateToken("alice");

        assertTrue(jwtUtil.validateToken(token, "alice"));
    }

    @Test
    void testValidateToken_RejectsDifferentUsername() {
        String token = jwtUtil.generateToken("alice");

        assertFalse(jwtUtil.validateToken(token, "bob"));
    }

    @Test
    void testExtractExpiration_ReflectsConfiguredLifetime() {
        long issuedAt = System.currentTimeMillis();
        String token = jwtUtil.generateToken("alice");

        Date expiration = jwtUtil.extractExpiration(token);
        assertTrue(expiration.after(new Date()));
        // JWT expiry has one-second resolution, so allow a second of slack either side
        long expectedExpiry = issuedAt + EXPIRATION_MS;
        assertTrue(Math.abs(expiration.getTime() - expectedExpiry) < 2000,
                "expected expiry near " + expectedExpiry + " but was " + expiration.getTime());
    }
}
