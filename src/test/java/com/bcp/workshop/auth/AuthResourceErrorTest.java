package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestProfile(AuthResourceErrorTest.MissingSigningKeyProfile.class)
class AuthResourceErrorTest {
    @Test
    void preservesDocumentedServerErrorWhenTokenSigningFails() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(500)
                .body("message", notNullValue())
                .body("exception", notNullValue());
    }

    public static class MissingSigningKeyProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "smallrye.jwt.sign.key.location", "missing-auth-test-signing-key.jwk",
                    "smallrye.jwt.verify.key.location", "test-signing-key.jwk"
            );
        }
    }
}