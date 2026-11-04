# 8. Exposed ports in the 7xxx range

Date: 2026-11-04

## Status

Accepted.

## Context

The project published the common defaults: 3000 for the Docker edge, 8080 for the API and 5173 for the Vite dev server. Many other tools and projects use those same ports, so they collide easily on a developer machine. A collision is worse with Playwright's `reuseExistingServer`, which reuses whatever is already listening on the port instead of failing.

## Decision

Every port PortWallet listens on from the host starts with 7, and keeps the last digits of the old default so it stays easy to recognize:

| Service | Old | New | Where |
| --- | --- | --- | --- |
| Web edge (Nginx, Docker Compose) | 3000 | **7000** | `WEB_PORT` in `docker-compose.yml` |
| API (Spring Boot) | 8080 | **7080** | `server.port` (`PORT`), Dockerfile, Nginx upstream, `docker-compose.dev.yml` |
| Frontend dev server (Vite) | 5173 | **7173** | `vite.config.ts` (`strictPort`), Playwright `baseURL`, `APP_BASE_URL` default |

The API listens on 7080 inside its container as well, so one number holds everywhere. Nginx keeps port 80 inside its container because it is never published directly.

## Consequences

- `WEB_PORT`, `PORT`, `API_URL` and `APP_BASE_URL` can still override these defaults.
- New services added later pick a free port in the 7xxx range and add a row to this table through a new ADR.
- The links in e-mails sent by the `dev` and `e2e` profiles point to `http://localhost:7173`, and in Docker Compose to `http://localhost:7000`.
