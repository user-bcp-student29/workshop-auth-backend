package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class AuthTokenLoggingTest {
    private static final String TEST_KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";

    @Inject
    JwtTokenService tokenService;

    @Test
    void doesNotLogTokensOrSigningKeyDuringSuccessfulAndFailedFlows() throws Exception {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        CapturingHandler handler = new CapturingHandler();
        rootLogger.addHandler(handler);
        String token = tokenService.create("student", "STUDENT").value();
        String[] tokenParts = token.split("\\.");
        tokenParts[2] = (tokenParts[2].startsWith("A") ? "B" : "A") + tokenParts[2].substring(1);

        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200);

            given()
                    .contentType("application/json")
                    .body("{\"username\":\"unknown\",\"password\":\"wrong\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(401);

            tokenService.verify(token);
            assertThrows(ParseException.class, () -> tokenService.verify(String.join(".", tokenParts)));
        } finally {
            rootLogger.removeHandler(handler);
        }

        String logs = String.join("\n", handler.messages());
        assertFalse(logs.contains(token));
        assertFalse(logs.contains(TEST_KEY));
    }

    private static final class CapturingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null && isLoggable(record)) {
                String message = new SimpleFormatter().formatMessage(record);
                if (record.getThrown() != null) {
                    StringWriter stackTrace = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(stackTrace));
                    message += stackTrace;
                }
                messages.add(message);
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}

        List<String> messages() {
            return messages;
        }
    }
}