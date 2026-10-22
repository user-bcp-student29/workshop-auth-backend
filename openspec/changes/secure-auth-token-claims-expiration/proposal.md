# Proposal

> Estado: APROBADO POR LA PERSONA USUARIA el 2026-10-21 para el Issue #13/H-04. `client` permanece opcional; el Issue #22/H-01 queda fuera de alcance.

## Why

[Issue #13](https://github.com/user-bcp-student29/workshop-auth-backend/issues/13) tracks confirmed finding H-04: login returns a Base64-encoded value without verifiable integrity, and its decoded content includes identity, expiry, and a fixed secret. The user approved this proposal and spec on 2026-10-21.

## Source And Confirmation Status

- Finding: H-04, [Token Base64 con secreto embebido y sin firma](../../../HALLAZGO.md), marked `Confirmado` by the user on 2026-10-21 and tracked by Issue #13.
- Evidence: `LegacyTokenUtil` encodes the username, role, expiry, and fixed secret as Base64 without a verifiable signature; `AuthResource` returns that value as `token`.
- The user's approval applies to this change for Issue #13/H-04. Issue #22/H-01 is outside this change.
- Related issue: [#13](https://github.com/user-bcp-student29/workshop-auth-backend/issues/13), with `Hallazgo origen = H-04`.

## What Changes

### In Scope

- Define the observable token contract for successful `POST /auth/login`: the token's integrity, authenticated identity, and expiry are verifiable, and its decoded content does not expose the embedded secret.
- Define verification outcomes: a valid token is accepted; an invalid, modified, or expired token is rejected at the token-verification boundary.
- Preserve the documented nominal lifetime and `expiresIn` value of 3600 seconds.
- Verify these outcomes without adding a new HTTP endpoint.

### Behavior To Preserve

- Keep `POST /auth/login` as the login endpoint and preserve the successful response fields `token`, `expiresIn`, `user` (including `username`, `displayName`, and `role`), and `debug`.
- Preserve the documented request behavior, including the optional and currently ignored `client` field; preserve the documented 400 responses for a null request or missing/blank username or password, the 401 response for invalid credentials, and the 500 response shape for unhandled failures.
- Leave `GET /q/health` and its documented behavior unchanged.

### Out Of Scope

- Requiring or adding a `client` claim, changing the optional `client` request behavior, or imposing a client allowlist.
- Changing credential storage, CORS, error handling, logging policy, or health behavior.
- Choosing a signing algorithm, token format/library, key-management approach, or other implementation architecture.

## Risks, Assumptions, And Open Questions

- The backend has no protected endpoint that consumes tokens. The verification criterion therefore applies at the existing token-verification boundary and must not be interpreted as adding an HTTP endpoint.
- No protected endpoint currently consumes tokens; verification is exercised at the token-verification boundary without adding an HTTP endpoint.

## Capabilities

### New Capabilities

- `auth-token`: Verifiable token integrity, identity and expiry, rejection of invalid or expired tokens, and preservation of the login contract.

### Modified Capabilities

None. The repository has no existing capability specs; this proposal introduces the token contract as a new capability.

## Impact

- The meaning and verifiability of `token` in the successful `POST /auth/login` response, while preserving its fields and the documented 3600-second `expiresIn`.
- Verification outcomes for valid, modified, invalid, and expired tokens.
- No dependencies or implementation approach are selected by this proposal.