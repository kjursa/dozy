# Authentication and login

## Current scope

Email-code authentication uses one canonical `ownerId` per account. Anonymous users also receive an `ownerId`, which the client stores and reuses. Anonymous and email accounts are separate; this release does not migrate or merge a library between them. After a successful email login, the mobile app clears its local library database.

The email sender is a development fake. Its verification code is always `123456789` (displayed as `123-456-789` in product notes); no real email is sent.

Codes contain nine digits, expire after ten minutes, allow at most three verification attempts per session, and allow at most five code requests per email per hour. Refresh tokens last six months, are rotated on use, and multiple sessions per account are supported. Logout revokes every refresh token for the account. Existing access tokens remain valid until their 20-minute expiry.

## Endpoints

All endpoints are under `/api/v1` and use JSON unless the response is empty.

| Method | Path | Request | Response |
| --- | --- | --- | --- |
| `POST` | `/auth/anonymous` | none | `{ "ownerId": "…" }` |
| `POST` | `/auth/email` | `{ "email": "…" }` | `{ "sessionId": "…" }` |
| `POST` | `/auth/email/code` | `{ "sessionId": "…", "code": "123456789" }` | `{ "ownerId": "…", "accessToken": "…", "refreshToken": "…" }` |
| `POST` | `/auth/refresh` | `{ "ownerId": "…", "refreshToken": "…" }` | `{ "accessToken": "…", "refreshToken": "…" }` |
| `POST` | `/auth/logout` | `{ "ownerId": "…" }` | empty `204` response |

`/auth/logout` revokes refresh tokens for all sessions owned by the supplied account. Issued access tokens are not revoked. Refresh requests require the matching owner ID and an active refresh token; each successful refresh invalidates that token and returns a replacement.

The backend stores accounts, code sessions, and hashed refresh tokens in SQLite. Access tokens are signed JWTs with a 20-minute lifetime. The mobile API client stores tokens through a platform-provided `AuthSessionStorage`, adds bearer access tokens to feature requests, and refreshes/retries after `401`. A rejected refresh token clears the local account session and emits `SessionState.Expired`; the app returns to login.

## Outside this release

- Sending real emails and production delivery configuration.
- Importing, merging, or restoring an anonymous library into an email account.
- Protecting and reassigning existing installation-owned data to account owners.
- Anonymous story quotas and anti-abuse controls.
- Session listing and revocation of a single device session.
