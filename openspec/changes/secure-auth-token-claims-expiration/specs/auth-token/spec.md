# Spec Delta

## Purpose

Defines the externally observable contract for authentication tokens issued by login, including identity and client claims, expiration, secret handling, and protection of sensitive values in logs.

## ADDED Requirements

### Requirement: Successful login issues a signed token with required claims
The system MUST return a verifiably signed token for successful login containing `sub`, `client`, `iat`, and `exp`. `sub` MUST identify the authenticated username, and `client` MUST equal the validated request value.

#### Scenario: Login returns an identifiable token
- **WHEN** a user submits valid credentials and a nonblank client value
- **THEN** the response is 200 and the token has a valid signature, `sub` equal to the authenticated username, `client` equal to the submitted value, and issue and expiry times with `exp` later than `iat`

### Requirement: Token lifetime and response duration agree
The system MUST preserve the existing nominal lifetime of 3600 seconds, and `expiresIn` MUST equal the remaining lifetime represented by `exp` within one second.

#### Scenario: Successful login reports the token's remaining lifetime
- **WHEN** a successful login response is produced
- **THEN** `expiresIn` represents `exp - now` within one second and the token's nominal lifetime is 3600 seconds

### Requirement: Client is validated for token issuance
The system MUST reject a login request with a missing or blank `client` rather than issue a token without a valid client claim. This changes the Day 1 request contract, which described `client` as optional and ignored.

#### Scenario: Client is missing
- **WHEN** the request has valid username and password but omits `client`
- **THEN** login returns 400 and no token is issued

#### Scenario: Client is blank
- **WHEN** the request has valid username and password but `client` is null, empty, or whitespace-only
- **THEN** login returns 400 and no token is issued

### Requirement: Expired tokens are rejected
The system MUST treat a token as invalid when its expiry time is at or before the time it is evaluated.

#### Scenario: Token is evaluated after expiry
- **WHEN** a token with an otherwise valid signature is evaluated at or after `exp`
- **THEN** the token is rejected as expired

### Requirement: Non-laboratory signing does not use an implicit secret
The system MUST NOT sign non-laboratory tokens with an implicit built-in default secret. Any retained laboratory-only default MUST be explicitly documented as laboratory-only.

#### Scenario: Non-laboratory token signing
- **WHEN** the service issues a token in a non-laboratory configuration
- **THEN** signing uses the configured `AUTH_TOKEN_SECRET` and not a built-in default

### Requirement: Sensitive token values are not logged
The system MUST NOT write authentication tokens or the signing secret to application logs during login or token evaluation.

#### Scenario: Successful login logging
- **WHEN** a login succeeds and its token is issued
- **THEN** application logs contain neither the returned token nor the signing secret

#### Scenario: Failed login logging
- **WHEN** login or token evaluation fails
- **THEN** application logs contain neither an authentication token nor the signing secret

### Requirement: Existing login response and error behavior is preserved
Except for the `client` requirement and token semantics defined above, login MUST preserve the Day 1 response fields and documented status behavior.

#### Scenario: Successful response retains documented fields
- **WHEN** a login succeeds
- **THEN** the 200 response retains `token`, `expiresIn`, `user.username`, `user.displayName`, `user.role`, and `debug`

#### Scenario: Existing request validation and credential failures
- **WHEN** the request is null or username or password is missing or blank
- **THEN** login returns the documented 400 response; when credentials are invalid, login returns the documented 401 response

#### Scenario: Unhandled login failure
- **WHEN** an unhandled error occurs during login
- **THEN** login retains the documented 500 response with `message` and `exception` fields