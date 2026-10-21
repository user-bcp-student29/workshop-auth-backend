# Tasks

Scope: GitHub Issue [#13](https://github.com/user-bcp-student29/workshop-auth-backend/issues/13) and the approved `auth-token` spec.

## 1. Characterize Current Login Behavior

- [ ] 1.1 Add endpoint characterization tests for the current successful response fields and documented null-request, missing/blank username or password, and invalid-credentials responses; verify them with `mvn -Dtest=AuthResourceTest test` before changing production behavior.
- [ ] 1.2 Characterize the current omitted-`client` acceptance and nominal 3600-second `expiresIn` without asserting or logging the legacy token contents; verify the focused test passes and retain the result as baseline evidence.

## 2. Establish JWT Signing and Verification

- [ ] 2.1 Add the Quarkus-supported SmallRye JWT capability and test-only signing configuration without a checked-in production secret; verify the application test profile starts with `mvn test`.
- [ ] 2.2 Replace the legacy token construction with signed `sub`, `client`, `iat`, and `exp` claims derived from one controllable clock instant; verify a focused test validates the signature, claim values, and 3600-second nominal lifetime.
- [ ] 2.3 Require externally supplied `AUTH_TOKEN_SECRET` outside tests and fail closed when it is absent or invalid; verify configuration tests demonstrate that non-test signing cannot fall back to a built-in secret.
- [ ] 2.4 Exercise token verification with valid, tampered, and expired tokens, including rejection at `exp`; verify focused verifier tests pass without adding a new HTTP endpoint.

## 3. Integrate the Contract Into Login

- [ ] 3.1 Require a nonblank `client` before issuing a token and preserve its exact submitted value in the claim; verify missing, null, empty, and whitespace-only values return 400 with no token, while a valid client produces the expected claim.
- [ ] 3.2 Preserve the documented successful response fields and existing 400/401/500 behavior while changing token format; verify the endpoint characterization tests from Task Group 1 pass after updating only the intentionally changed omitted-client expectation.
- [ ] 3.3 Add log-capture coverage for successful and failed login/token evaluation and prevent token or secret values from appearing; verify the tests inspect captured application logs without printing the sensitive values.
- [ ] 3.4 Update `docs/ENDPOINTS.md` with the required `client`, signed-token claims, `expiresIn` tolerance, and `AUTH_TOKEN_SECRET` configuration; verify each documented behavior matches the approved spec and the issue's scope.

## 4. Integration Verification

- [ ] 4.1 Run the full Maven test suite and `openspec validate secure-auth-token-claims-expiration`; verify both complete successfully and all protected response/error contracts remain covered.