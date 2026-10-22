package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class JwtTokenServiceTest {
    private static final Instant ISSUED_AT = Instant.parse("2030-01-01T00:00:00Z");

    @Inject
    JWTParser parser;

    @Test
    void createsSignedTokenWithIdentityAndExpiryFromOneClockInstant() throws Exception {
        JwtTokenService service = new JwtTokenService(parser, Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

        JwtTokenService.IssuedToken issued = service.create("student", "STUDENT");
        JsonWebToken verified = service.verify(issued.value());

        assertEquals("student", verified.getSubject());
        assertEquals("STUDENT", verified.getClaim("role"));
        assertNull(verified.getClaim("client"));
        assertEquals(ISSUED_AT.getEpochSecond(), verified.getIssuedAtTime());
        assertEquals(ISSUED_AT.plusSeconds(JwtTokenService.TOKEN_TTL_SECONDS).getEpochSecond(), verified.getExpirationTime());
        assertEquals(JwtTokenService.TOKEN_TTL_SECONDS, issued.expiresIn());
    }

    @Test
    void rejectsTokenWithModifiedSignature() {
        JwtTokenService service = new JwtTokenService(parser, Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
        String token = service.create("student", "STUDENT").value();
        String[] tokenParts = token.split("\\.");
        tokenParts[2] = (tokenParts[2].startsWith("A") ? "B" : "A") + tokenParts[2].substring(1);

        assertThrows(ParseException.class, () -> service.verify(String.join(".", tokenParts)));
    }

    @Test
    void rejectsTokenAtItsExpiryBoundary() {
        Instant expiryBoundary = Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS);
        Instant issuedAt = expiryBoundary.minusSeconds(JwtTokenService.TOKEN_TTL_SECONDS);
        JwtTokenService issuer = new JwtTokenService(parser, Clock.fixed(issuedAt, ZoneOffset.UTC));
        JwtTokenService verifier = new JwtTokenService(parser, Clock.fixed(expiryBoundary, ZoneOffset.UTC));
        String token = issuer.create("student", "STUDENT").value();

        assertThrows(IllegalArgumentException.class, () -> verifier.verify(token));
    }
}