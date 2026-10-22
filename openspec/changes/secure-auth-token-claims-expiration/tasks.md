# Tasks

Status: APPROVED by the user on 2026-10-21 for Issue #13/H-04. `client` remains optional and ignored. Issue #22/H-01 is out of scope.

Scope: GitHub Issue [#13](https://github.com/user-bcp-student29/workshop-auth-backend/issues/13) and the approved `auth-token` spec.

## 1. Characterize Current Login Behavior

- [x] 1.1 Add endpoint characterization tests for the current successful response fields and documented null-request, missing/blank username or password, and invalid-credentials responses; verified with `mvn -Dtest=AuthResourceTest test` before changing production behavior.
- [x] 1.2 Characterize the current omitted-`client` acceptance and nominal 3600-second `expiresIn` without asserting or logging the legacy token contents; the focused test passes and the result is recorded below.

Execution evidence: `mvn -Dtest=AuthResourceTest test` — Surefire reports 10 tests, 0 failures, 0 errors, 0 skipped. The run emitted JaCoCo/JDK instrumentation and JBoss Threads module-access warnings, but the test set completed successfully. The pre-change baseline was 1 test, 0 failures, 0 errors, 0 skipped.

## 2. Establish JWT Signing and Verification

- [x] 2.1 Add the Quarkus-supported SmallRye JWT capability and test-only HS256 signing configuration without a checked-in production secret; verified the application test profile starts with `mvn test` (10 tests, 0 failures, 0 errors, 0 skipped).
- [x] 2.2 Replace the legacy token construction with signed identity and expiry claims derived from one controllable clock instant; verified with `mvn -Dtest=JwtTokenServiceTest test` (1 test, 0 failures, 0 errors). Do not add a `client` claim.
- [x] 2.3 Require externally supplied `AUTH_TOKEN_SECRET` outside tests and fail closed when it is absent or invalid; verified with `mvn '-Dtest=JwtTokenServiceTest,JwtTokenConfigurationTest,JwtTokenMissingSecretTest' test` (5 tests, 0 failures, 0 errors), covering valid key, invalid key location, and absent secret.
- [x] 2.4 Exercise token verification with valid, tampered, and expired tokens, including rejection at `exp`; verified with `mvn -Dtest=JwtTokenServiceTest test` (3 tests, 0 failures, 0 errors), without adding an HTTP endpoint.

## 3. Integrate the Contract Into Login

- [x] 3.1 Preserve `client` as optional and ignored during login and token issuance; verified with `mvn '-Dtest=AuthResourceTest,JwtTokenServiceTest' test` (13 tests, 0 failures, 0 errors), including successful login with/without `client` and absence of the claim.
- [x] 3.2 Preserve the documented successful response fields and existing 400/401/500 behavior while changing token format; verified with `mvn '-Dtest=AuthResourceTest,AuthResourceErrorTest' test` (11 tests, 0 failures, 0 errors). The omitted-`client` expectation remains unchanged.
- [x] 3.3 Add log-capture coverage for successful and failed login/token evaluation and prevent token or secret values from appearing; verified with `mvn -Dtest=AuthTokenLoggingTest test` (1 test, 0 failures, 0 errors). Captured logs contain neither the issued token nor test key material.
- [x] 3.4 Update `docs/ENDPOINTS.md` with the optional, ignored `client` behavior, signed-token identity and expiry claims, 3600-second `expiresIn`, and `AUTH_TOKEN_SECRET` configuration; verified against the approved spec and Issue #13 scope with `mvn '-Dtest=AuthResourceTest,AuthResourceErrorTest' test` (11 tests passed).

## 4. Integration Verification

- [x] 4.1 Run the full Maven test suite and `openspec validate secure-auth-token-claims-expiration`; both passed. Surefire: 17 tests, 0 failures, 0 errors, 0 skipped; OpenSpec validation: 1 change passed, 0 failed.