# Backend TODO

- [ ] Add `POST /internal/cleanup` to remove stale database records. Run it daily
  from an external cron job on the backend host. Have cron call Ktor directly
  over loopback and configure the public reverse proxy to return HTTP 404 for
  `/internal` and `/internal/*`. Keep the upstream port private.
  - Remove expired refresh tokens and any revoked records still present.
  - Remove email code records only after they have expired and fallen outside
    the configured send rate limit window. Used codes still count within that window.
  - Make cleanup idempotent so retries are safe.
