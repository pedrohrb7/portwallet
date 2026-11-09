# 9. Deploy with Woodpecker CI and Docker Swarm

Date: 2026-11-09

## Status

Superseded by [ADR 0010](0010-build-and-deploy-from-github-actions.md).

## Context

Phase 8 asks for a public deploy of the demo. The project already has a VPS with a single-node Docker Swarm, Traefik v3 (TLS from Let's Encrypt) and Woodpecker CI v3.18.1 at `https://ci.pedrohrb.dev`. GitHub Actions (`.github/workflows/ci.yml`) already proves build, unit and integration tests, E2E and the Docker Compose smoke test on every push to `main`, but it does not publish images or deploy anything.

The deploy must not hand the VPS to the pipeline: whoever controls a pipeline step should not be able to run arbitrary commands on the server.

## Decision

- **Two pipelines, two jobs.** GitHub Actions keeps testing; it does not change. Woodpecker (`.woodpecker.yml`) only packages and deploys, on every push to `main` that touches more than `docs/`, `*.md` or `.github/`, and on manual runs.
- **Images in GHCR.** Woodpecker builds `ghcr.io/pedrohrb7/portwallet-api` from `backend/` and `ghcr.io/pedrohrb7/portwallet-web` from `frontend/` with `woodpeckerci/plugin-docker-buildx`, and tags each with `latest` and the commit SHA. The plugin version is pinned to the one the server allows as privileged.
- **Restricted deploy over SSH.** The deploy steps (`appleboy/drone-ssh`, pinned) log in as the `deploy` user, which accepts a single command: `deploy <stack>_<service> <image>`. It only swaps the image of a service that already exists, so each service gets its own step, the API first because the web container proxies to it. Each deploy uses the commit SHA tag, never `latest`, so the running version is always traceable to a commit. The VPS host key is pinned by its SHA-256 fingerprint.
- **Stack and secrets live on the VPS.** The stack definition (services, networks, Traefik labels) and the secrets are on the server, not in this repository. The pipeline cannot create, remove or reconfigure services, and the first deploy of the stack is done on the server.
- **Runtime shape.** Traefik terminates TLS for `https://portwallet.pedrohrb.dev` and talks to `web` (Nginx, port 80); `web` talks to `api` (port 7080) on an internal network, and `api` is not exposed. The API runs in the `demo` profile (simulated inbox and Swagger UI) and the web image is built with `VITE_DEMO_INBOX=true`: there is no real e-mail delivery, so the simulated inbox is the only way to activate an account.
- **Secrets as Docker secrets.** `JWT_SECRET` and `PORTWALLET_CRYPTO_*` are Docker secrets, read by Spring through `SPRING_CONFIG_IMPORT=optional:configtree:/run/secrets/`. The images carry no secret and no default for them.
- **Forwarded scheme.** Nginx passes on the `X-Forwarded-Proto` it receives from Traefik (and falls back to its own scheme when nothing is in front of it, as in Docker Compose), and the API runs with `SERVER_FORWARD_HEADERS_STRATEGY=framework`. Without that, the API sees `http`, and Swagger UI builds `http://` URLs that the browser blocks as mixed content.

## Consequences

- The deploy credentials (`ghcr_token`, `deploy_ssh_key`, `deploy_host`) live only in Woodpecker on the VPS, never in GitHub. Even a stolen deploy key can only point an existing service to another `ghcr.io/pedrohrb7/` image.
- The pipelines are independent: Woodpecker publishes and deploys even when GitHub Actions fails. If that becomes a problem, the backend tests can run in Woodpecker before the build, at the cost of slower builds on the VPS.
- The buildx plugin starts its own Docker daemon, so every build starts without a layer cache and downloads the Maven and npm dependencies again.
- The database is still in-memory H2 ([ADR 0002](0002-foundation-stack-and-structure.md)): every deploy starts with an empty database.
- The simulated inbox (`/mock/inbox?to=<e-mail>`) is public: whoever knows someone's e-mail sees their activation link and, by [ADR 0005](0005-password-set-on-activation.md), sets their password. That is acceptable for a demo with fictional data, so the README's link to the demo must warn people not to use a real CPF or real data, and that the data is erased on every deploy.
- The repository is public, so pipelines from forks need approval in Woodpecker, and the repository is not marked as trusted.
- In plugin 6.x, `build_args` is read as JSON: it must be a map with string values (`VITE_DEMO_INBOX: "true"`). A `KEY=value` list or an unquoted boolean never reaches the build under that name.
