# Spec Delta

## Purpose

Approved requirement for the externally observable integrity, identity, and expiry behavior of the authentication token issued by login, based on confirmed finding H-04. Approved by the user on 2026-10-21 for Issue #13; Issue #22/H-01 is outside scope.

## ADDED Requirements

### Requirement: Successful login issues a token with verifiable integrity
The system MUST return a token whose integrity and authenticated identity can be verified. The token MUST represent the identity authenticated by `POST /auth/login`, provide a verifiable expiry, and MUST NOT expose the embedded secret as decodable token data.

#### Scenario: Successful login returns a verifiable token
- **WHEN** a user submits valid credentials
- **THEN** the response is 200 and the returned token has verifiable integrity, represents the authenticated user's identity, and provides a verifiable expiry
- **AND** decoding the token does not disclose the embedded secret

### Requirement: Invalid or expired tokens are rejected
At the token-verification boundary, the system MUST accept a valid token and reject an invalid, modified, or expired token.

#### Scenario: Valid token is verified
- **WHEN** a token with valid integrity and an unexpired expiry is evaluated
- **THEN** it is accepted and its authenticated identity can be verified

#### Scenario: Modified or invalid token is rejected
- **WHEN** a token is modified or otherwise fails integrity verification
- **THEN** it is rejected

#### Scenario: Expired token is rejected
- **WHEN** a token is evaluated at or after its expiry
- **THEN** the token is rejected as expired

### Requirement: Login contract is preserved apart from token semantics
The system MUST preserve the documented `POST /auth/login` endpoint, request behavior, response fields, and status behavior. The token's integrity and verifiability are the only behavior changed by this specification.

#### Scenario: Successful response retains documented fields
- **WHEN** a login succeeds
- **THEN** the 200 response retains `token`, `expiresIn` with its documented nominal value of 3600 seconds, `user.username`, `user.displayName`, `user.role`, and `debug`

#### Scenario: Existing request and credential failures
- **WHEN** the request is null or username or password is missing or blank
- **THEN** login returns the documented 400 response; when credentials are invalid, login returns the documented 401 response

#### Scenario: Unhandled login failure
- **WHEN** an unhandled error occurs during login
- **THEN** login retains the documented 500 response with `message` and `exception` fields