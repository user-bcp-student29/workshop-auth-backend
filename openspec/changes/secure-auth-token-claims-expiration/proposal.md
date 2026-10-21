# Proposal

## Why

[Issue #13](https://github.com/user-bcp-student29/workshop-auth-backend/issues/13) reports that login currently returns a Base64-encoded value rather than a signed token with verifiable identity and expiry claims. The change makes the token's identity, client, issue time, and expiration explicit and testable, and reduces the risk of relying on an embedded secret or exposing sensitive values in logs.

## What Changes

### In Scope

- Define the token contract for successful `POST /auth/login`: a signed token identifies the authenticated user and contains `sub`, `client`, `iat`, and `exp` claims.
- Validate the `client` supplied in the request and include that same client in the token.
- Keep the existing nominal token lifetime of 3600 seconds and define `expiresIn` as the remaining lifetime represented by `exp`, within a one-second tolerance.
- Require nonblank `client` for a successful login. This is an intentional **BREAKING** change from the Day 1 contract, where `client` is optional and ignored; a missing or blank value is proposed to follow the existing 400 validation behavior. The Issue requires a validated client claim but does not specify a client allowlist, so this proposal assumes nonblank is the validation boundary.
- Ensure the secret is not silently relied on outside the laboratory; if a laboratory default is retained, identify it explicitly as laboratory-only. Do not log tokens, secrets, or other sensitive values.
- Add observable coverage for successful login, invalid credentials, and expired-token rejection without introducing a new HTTP endpoint.

### Behavior To Preserve

- Keep `POST /auth/login` as the login endpoint and preserve the successful response fields `token`, `expiresIn`, `user` (including `username`, `displayName`, and `role`), and `debug`.
- Preserve the documented 400 responses for a null request or missing/blank username or password, the 401 response for invalid credentials, and the documented 500 response shape for unhandled failures. The Issue does not request changes to these flows.
- Leave `GET /q/health` and its documented behavior unchanged.

### Out Of Scope

- Choosing HMAC versus a JWT library, token serialization, key rotation, or other implementation architecture. The Issue asks that the choice be documented, but that decision is deferred to design.
- Adding protected endpoints, changing the health endpoint, changing CORS, or revising unrelated error handling.

### Assumptions To Confirm

- The one-second `expiresIn` tolerance accommodates second-granularity expiry claims and response timing; the Issue requires a documented tolerance but gives no value.
- `sub` identifies the authenticated user's existing username. No additional client allowlist is introduced without a source requirement.

## Capabilities

### New Capabilities

- `auth-token`: Signed authentication-token claims, expiry semantics, client validation, secret handling, and sensitive-log behavior.

### Modified Capabilities

None. The repository has no existing capability specs; this proposal introduces the token contract as a new capability.

## Impact

- API behavior and contract documentation for `POST /auth/login`, specifically the formerly optional `client` field and the meaning of `token` and `expiresIn`.
- Authentication token generation/configuration and the login tests.
- Security-related logging and configuration documentation for `AUTH_TOKEN_SECRET`.
- No dependencies or implementation approach are selected by this proposal.