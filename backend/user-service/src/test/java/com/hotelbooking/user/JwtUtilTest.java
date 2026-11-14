package com.hotelbooking.user;

import com.hotelbooking.user.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "TestSecretKeyForUnitAndIntegrationTestsWithMinimum256BitsLength1234567890";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
        jwtUtil.init();
    }

    @Test
    void testGenerateAndValidateToken_Success() {
        String token = jwtUtil.generateToken("test@example.com", "USER", 1L);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertEquals(1L, jwtUtil.extractUserId(token));
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.jwt.token"));
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testValidateToken_TamperedToken_ReturnsFalse() {
        String token = jwtUtil.generateToken("test@example.com", "USER", 1L);
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void testInit_ShortSecret_ThrowsIllegalStateException() {
        JwtUtil invalidJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(invalidJwtUtil, "secretKey", "short-secret");
        ReflectionTestUtils.setField(invalidJwtUtil, "expirationMs", 86400000L);

        assertThrows(IllegalStateException.class, invalidJwtUtil::init);
    }
}
