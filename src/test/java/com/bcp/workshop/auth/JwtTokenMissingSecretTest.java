package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestProfile(JwtTokenMissingSecretTest.MissingSecretProfile.class)
class JwtTokenMissingSecretTest {
    @Inject
    JwtTokenService tokenService;

    @Test
    void doesNotIssueTokenWhenExternalSecretIsAbsent() {
        assertThrows(RuntimeException.class, () -> tokenService.create("student", "STUDENT"));
    }

    public static class MissingSecretProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "AUTH_TOKEN_SECRET", "",
                    "smallrye.jwt.verify.key.location", "test-signing-key.jwk"
            );
        }
    }
}