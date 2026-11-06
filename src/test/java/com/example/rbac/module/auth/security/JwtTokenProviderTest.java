package com.example.rbac.module.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    @Test
    void generatesAndParsesToken() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret",
                "test-secret-that-is-long-enough-for-hs512-signing-0123456789abcdef");
        ReflectionTestUtils.setField(provider, "expiration", 3600L);

        String token = provider.generateToken("admin", 7, 3);

        assertTrue(provider.validateToken(token));
        assertEquals("admin", provider.getUsername(token));
        assertEquals(7, provider.getUserId(token));
        assertEquals(3, provider.getOrgId(token));
        assertEquals(3600L, provider.getExpirationSeconds());
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret",
                "test-secret-that-is-long-enough-for-hs512-signing-0123456789abcdef");
        ReflectionTestUtils.setField(provider, "expiration", 3600L);

        String token = provider.generateToken("admin", 1, 1);

        assertFalse(provider.validateToken(token + "tampered"));
    }
}
