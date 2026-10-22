package com.bcp.workshop.auth;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class JwtTokenService {
    static final long TOKEN_TTL_SECONDS = 3600;

    private final JWTParser parser;
    private final Clock clock;

    @Inject
    public JwtTokenService(JWTParser parser) {
        this(parser, Clock.systemUTC());
    }

    JwtTokenService(JWTParser parser, Clock clock) {
        this.parser = parser;
        this.clock = clock;
    }

    public IssuedToken create(String username, String role) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        long issuedAtSeconds = issuedAt.getEpochSecond();
        long expiresAtSeconds = issuedAt.plusSeconds(TOKEN_TTL_SECONDS).getEpochSecond();

        String value = Jwt.issuer("workshop-auth")
                .subject(username)
                .claim("role", role)
                .issuedAt(issuedAtSeconds)
                .expiresAt(expiresAtSeconds)
                .sign();

        return new IssuedToken(value, (int) (expiresAtSeconds - issuedAtSeconds));
    }

    public JsonWebToken verify(String value) throws ParseException {
        JsonWebToken verified = parser.parse(value);
        if (clock.instant().getEpochSecond() >= verified.getExpirationTime()) {
            throw new IllegalArgumentException("Token expired");
        }
        return verified;
    }

    public record IssuedToken(String value, int expiresIn) {}
}