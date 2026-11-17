# Authentication and login plan

This document records the current product and technical direction for authentication. Items marked **Open** still need a decision before implementation.

## Product direction

- Keep the app usable anonymously; do not require an account at first launch or before story generation.
- Anonymous users have an initial limit of three stories. Treat this as a product limit for an anonymous owner, not as a strong anti-abuse control: reinstalling the app may create a new anonymous owner.
- Offer account login as a way to preserve and restore a library. Do not rely on email accounts as proof of a unique person or as a complete abuse-prevention mechanism.
- Use email verification codes for sign-in. No password is planned.
- An account has one canonical `ownerId` and one library in the initial version. Multi-device login restores that same library.

## Anonymous and account identities

- Keep the current UUIDv7 `ownerId` model for anonymous data. The identifier is a bearer credential for the anonymous library: possession grants access, so clients and servers must not put it in logs or analytics.
- On first verification of an email, associate the account with the current anonymous `ownerId`. Existing records keep their IDs and ownership; no data migration is needed.
- When the verified email already belongs to an account with a different `ownerId`, do not merge the current anonymous library automatically. Ask the user to confirm switching to the existing account's library.
- Confirmation screen copy (Polish):

  > To konto już istnieje. Bajki z obecnej, lokalnej biblioteki zostaną zastąpione kolekcją przypisaną do tego konta.

  Buttons: **Anuluj** and **Kontynuuj**.

- On confirmation, switch to the canonical account `ownerId` and fetch its library. Keep the local anonymous database until the account library has been fetched successfully; a failed login or download must not destroy local data.
- The anonymous library is not merged into the existing account in this version. The confirmation must make that consequence clear.
- A later product decision may add an explicit import/merge flow if users need to combine libraries from multiple anonymous devices.

## Tokens and sessions

- Issue a short-lived JWT access token after successful email verification. Current target lifetime: 20 minutes.
- Issue a cryptographically random refresh token and store only a server-side HMAC hash of it, using a key separate from the JWT signing key when configured.
- Refresh requests present the refresh token; the server resolves the account/session from it. Do not require a client-supplied `userId` to refresh.
- Rotate refresh tokens on use. Define session revocation and replay handling as part of implementation.
- JWTs use a symmetric HMAC signature. The proposed simple key derivation is:

  ```text
  jwtBase = HMAC-SHA-256(appSecret, "v1:jwt")
  jwtKey  = HMAC-SHA-256(jwtBase, kid)
  ```

- `kid` may identify a monthly key period (for example `11-2026`). Validate the allowed algorithm, `kid`, issuer, audience, subject, and time claims. Retain verification support for unexpired tokens signed with a previous period key.
- This derivation does not protect against compromise of `appSecret`; it is a simple way to derive separate, context-bound keys without introducing Vault initially.
- **Open:** Decide whether the first implementation uses one `APP_SECRET` with derived keys, or separate environment variables such as `JWT_SECRET` and `REFRESH_TOKEN_HASH_SECRET`. Keep secret loading behind configuration so the choice can change without changing auth use cases.
- **Open:** Confirm refresh-token lifetime. Six months was discussed as a possible target, not a final decision.
- **Open:** Confirm email-code length, expiration, resend limits, and attempt limits. Six digits and about ten minutes were suggested, with strict rate limits.

## Backend implementation direction

- Implement auth as a feature under `features/auth`, following the existing feature-oriented architecture.
- Keep account/session persistence behind feature-owned repository contracts. Use fakes in focused tests; production auth state must use durable storage.
- Configure Ktor authentication centrally in shared plugin/configuration code. Routes declare authentication requirements; application operations enforce ownership and authorization rules.
- Do not trust a client-supplied target owner ID for authenticated requests. Resolve the canonical owner from the authenticated principal.
- Keep email codes, access tokens, refresh tokens, and secrets out of logs.
- Before production, choose durable database and deployment configuration. The backend currently uses SQLite, while `DB.md` documents PostgreSQL as a future design. Docker Compose with PostgreSQL (and Redis only if it has a defined responsibility) is a deployment step, not a prerequisite for designing auth contracts.

## Mobile behavior

- Anonymous use remains available without login.
- When the user signs in with an email that already has an account and the device has a local anonymous library, show the confirmation screen before switching libraries.
- Cancel keeps the current anonymous identity and local library.
- Continue switches to the account identity, downloads the account library, and only then may discard or replace the local anonymous data.
- Refreshing access tokens must be coordinated so concurrent expired requests share one refresh operation.

## Decisions still needed

1. Email provider and delivery integration.
2. Email-code format, expiry, resend throttling, and verification-attempt limits.
3. Refresh-token lifetime, session listing/revocation, and replay response.
4. Whether to start with a single application secret or separate purpose-specific secrets.
5. Whether the existing SQLite backend is acceptable for the first auth release or PostgreSQL should be introduced first.
6. Exact anonymous story quota behavior and whether the limit includes failed/deleted generations.
7. Whether switching to an existing account should preserve the old anonymous library as an inaccessible local backup or remove it after successful download.
