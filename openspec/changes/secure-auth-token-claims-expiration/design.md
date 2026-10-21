# Design

## Context

See [proposal.md](proposal.md) for motivation and scope, and [specs/auth-token/spec.md](specs/auth-token/spec.md) for the normative behavior.

`POST /auth/login` currently validates username and password, authenticates through the legacy database, and returns a token plus `expiresIn`, `user`, and `debug`. The token utility constructs a Base64-encoded string containing username, role, expiry, and a source-code secret; this is neither a signed token nor a verifiable claim set. `application.properties` does not configure `AUTH_TOKEN_SECRET`, despite the Issue describing it as an existing environment setting. The only current endpoint test covers a successful login and a non-null token. The Day 1 contract documents the response and error statuses, and describes `client` as optional and ignored.

## Goals / Non-Goals

**Goals:**

- Introduce a standard, verifiable signed-token format without adding a token-consumer endpoint.
- Make signing configuration explicit and fail closed when a non-test environment lacks its secret.
- Preserve the documented login response and errors except for the deliberate `client` requirement and the token semantics in the spec.
- Make time-dependent claim and expiry behavior deterministic in tests.

**Non-Goals:**

- Add authorization middleware, protected resources, refresh tokens, key rotation, or a new login flow.
- Change credential storage, database behavior, CORS, health reporting, or the documented 500 response in this change.
- Preserve compatibility for consumers that decode the current Base64 token as a private format; the new token is a deliberate format change.

## Protected Contracts

- Preserve `POST /auth/login`, its 200 response fields (`token`, `expiresIn`, `user.username`, `user.displayName`, `user.role`, and `debug`), and the documented 400, 401, and 500 response behavior.
- Preserve the existing 3600-second nominal lifetime. `expiresIn` and the token expiry must agree within the one-second tolerance in the spec.
- Require `client` only for successful token issuance, as specified. Missing/blank `client` is the one intentional request-compatibility break from Day 1; do not add a client allowlist or normalize the value silently.
- Keep `/q/health` unchanged.

## Components Affected

- Login request validation and response assembly in the authentication resource.
- The legacy token utility, replaced by a token issuer/verifier boundary.
- Quarkus JWT support and signing configuration, with `AUTH_TOKEN_SECRET` supplied by the environment outside tests.
- Login/token tests and `docs/ENDPOINTS.md`, which must reflect the intentional `client` and token contract changes.

The database model and health endpoint are not affected.

## Decisions

### Use a standard JWT implementation with HMAC signing

Use Quarkus-supported SmallRye JWT capabilities for token construction and verification, with HS256 signing. This keeps the existing HMAC intent while delegating serialization, signature verification, and standard `iat`/`exp` handling to a maintained JWT implementation. The repository already runs on Quarkus, but currently has no JWT dependency.

Alternatives considered:

- Keep the custom Base64/HMAC string: rejected because Base64 is not a token signature format and the current value exposes its fields and embedded secret.
- Hand-roll a compact token and HMAC: rejected because custom encoding and verification create avoidable parsing, canonicalization, and expiry-validation risks.
- Use asymmetric signing: not selected because this service has no current token-consumer endpoint or key-distribution setup, while the Issue calls out a secret-based configuration. Revisit if independent verifiers must validate tokens without sharing a secret.

### Use an environment-provided secret with no built-in fallback

Treat `AUTH_TOKEN_SECRET` as external configuration. The repository implementation does not currently read it, so the change must establish that configuration rather than assume it exists. Non-test signing must fail closed when the secret is absent or invalid; no source-code or packaged default is permitted. Tests use an isolated test-only secret that is never used by non-test profiles. Provision a sufficiently strong secret through deployment configuration, not checked-in files or logs.

Alternatives considered:

- Retain and label the existing literal as laboratory-only: rejected because the current application has no explicit laboratory profile boundary and the same artifact could otherwise be deployed outside it.
- Use a checked-in development secret: rejected because profile/config mistakes could promote it into a deployed environment.

### Derive all time claims from one clock instant

Use one UTC clock instant at whole-second precision to derive `iat` and `exp`, with `exp` 3600 seconds after `iat`. Derive `expiresIn` from the same expiry and captured instant, and make the clock controllable in tests. This avoids boundary flakiness and keeps the response duration aligned with the signed claim. Apply no expiry leeway: a token is expired at or after `exp`, as required by the spec.

Alternatives considered:

- Call the system clock separately for issue time, expiry, and response duration: rejected because rounding and call timing can produce intermittent mismatches at the one-second boundary.
- Add a clock-skew grace period: rejected because it conflicts with the explicit expiry boundary in the spec.

### Keep claim mapping narrow and client validation explicit

Set `sub` to the authenticated username and `client` to the exact supplied value after rejecting null, empty, or whitespace-only input. Do not trim or rewrite a valid client value, and do not invent an allowlist. Preserve the existing response `user.role`; the token does not gain claims beyond those required by the spec.

Alternatives considered:

- Make `client` optional for compatibility: rejected because it cannot satisfy the required client claim and the approved spec records the breaking change.
- Normalize or allowlist clients: rejected because neither behavior is required by the Issue or current contract.

### Verify expiry at the token-verification boundary, not through a new endpoint

Use the JWT verifier's signature and expiry checks wherever a token is evaluated. Do not add a protected endpoint or global authorization filter in this change; the current application has no token-consuming route. Exercise the verifier directly with valid, tampered, and expired tokens so the expiry contract is testable without broadening the application surface.

Alternatives considered:

- Add a validation endpoint solely to demonstrate expiry: rejected as unnecessary API surface.
- Test only the emitted `exp` claim: rejected because that would not prove expired tokens are rejected by verification.

## Characterization Before Behavior Changes

Before changing login or token behavior, establish and record the existing baseline with the current test suite and focused API characterization tests. Cover:

- A successful login's status and documented response fields, including `user` and `debug`.
- The documented 400 behavior for null request and missing/blank username or password, plus 401 for invalid credentials.
- The current acceptance of an omitted `client`, explicitly as baseline evidence only; replace this expectation with the specified 400 behavior when the intentional breaking change is introduced.
- The current token lifetime/response behavior without emitting tokens or secrets to test logs.

Do not freeze the old Base64 representation as a permanent compatibility requirement. After the baseline is captured, update/add tests for signature verification and required claims, exact client mapping, 3600-second expiry and `expiresIn` tolerance, expiry rejection at the boundary, missing-secret failure outside tests, and log redaction. Retain characterization coverage for the protected response and error contracts throughout.

## Incremental Strategy

1. Capture the current test baseline and characterization results before changing authentication behavior.
2. Add the JWT capability and isolated test configuration; prove signing and verification, including expiry, independently of the login flow.
3. Replace token issuance, make secret configuration fail closed outside tests, and then wire the validated client into login. Keep response fields and existing status behavior covered while changing the token format.
4. Update endpoint documentation and run the full relevant test suite, including a log-capture check for token/secret leakage.

## Risks / Trade-offs

- **Existing clients may decode the legacy Base64 value or omit `client`** -> Treat the format and required-client change as a documented breaking contract; identify consumers and coordinate their update before rollout. Existing responses are not silently dual-formatted.
- **A weak or missing HMAC secret can undermine every token** -> Require externally provisioned strong secret material and fail closed outside tests; never log its value.
- **JWT support adds dependencies and configuration surface** -> Use the Quarkus-managed SmallRye JWT integration and keep the token boundary limited to login issuance and verification.
- **No current API consumes tokens** -> Verify expiry directly at the token-verification boundary; do not imply that login alone protects future endpoints.
- **The current 500 body exposes exception information** -> Preserve it for this scoped change as required by the existing contract; track its remediation separately rather than silently changing behavior here.
- **Characterization can accidentally preserve insecure behavior** -> Use baseline tests to document current behavior before the change, then explicitly replace only the assertions that conflict with the approved spec.

## Migration Plan and Rollback

Provision `AUTH_TOKEN_SECRET` in the target environment before deploying the new issuer. Deploy the new issuer and its verifier together; consumers must treat the new token as an opaque signed JWT and clients must send a nonblank `client`. No database migration is required. Previously issued legacy tokens are not accepted by the new verifier and clients must obtain new tokens after deployment.

Rollback is conditional. In an isolated laboratory, the previous release can be redeployed if needed, with the understanding that it restores the insecure legacy token behavior. Do not roll back a production deployment to the embedded-secret implementation. If the new release cannot operate safely, disable login or roll forward with a fix; preserve the configured secret and avoid logging it. Because the application currently has no token-consuming endpoint, there is no server-side token store to migrate or restore.