# 10. Build and deploy from GitHub Actions

Date: 2026-11-09

## Status

Accepted. Supersedes [ADR 0009](0009-deploy-with-woodpecker-and-swarm.md).

## Context

[ADR 0009](0009-deploy-with-woodpecker-and-swarm.md) split delivery between two pipelines: GitHub Actions tested, and Woodpecker CI on the VPS built the images, pushed them to GHCR and deployed them. In practice that setup had four problems:

- The pipelines were independent, so Woodpecker published and deployed even when the tests failed in GitHub Actions.
- Pushing to GHCR from Woodpecker needed a personal access token, which expired every 90 days.
- The image builds ran on the VPS, competing with the services it hosts, and always started without a layer cache.
- Woodpecker was one more service to run, update and secure on the server.

## Decision

- **One workflow.** `.github/workflows/ci.yml` runs the tests, then `images`, then `deploy`. `images` follows the same rule as the E2E job (the app changed and no check that ran failed or was cancelled), and `deploy` runs only when both images were pushed, so a failing test never reaches production. Both jobs also require `github.ref == 'refs/heads/main'`, so another trigger added to the workflow later cannot publish or deploy from another branch. `.woodpecker.yml` is removed.
- **Images in GHCR with the `GITHUB_TOKEN`.** The `images` job builds `ghcr.io/pedrohrb7/portwallet-api` from `backend/` and `ghcr.io/pedrohrb7/portwallet-web` from `frontend/` (with `VITE_DEMO_INBOX=true`) using `docker/build-push-action`, and tags each with `latest` and the commit SHA. It logs in with the job's `GITHUB_TOKEN` (`packages: write`), so there is no token to rotate. Both packages stay private and grant this repository Write access under *Manage Actions access*.
- **Same restricted deploy over SSH.** The `deploy` job logs in as the `deploy` user with the same restricted key as before, which accepts a single command per connection: `deploy <stack>_<service> <image>`. The API goes first, then the web, each by its commit SHA tag. The key is the `DEPLOY_SSH_KEY` secret of the GitHub environment `production`, which only accepts deployments from `main`. The VPS's ED25519 host key is pinned in the workflow and checked with `StrictHostKeyChecking=yes`.
- **Third-party actions pinned by commit**, like `dorny/paths-filter`, so a moved tag cannot change what runs.
- **Unchanged from ADR 0009:** the stack definition and its secrets live on the server (managed in Portainer), and so do the runtime shape, the Docker secrets and the forwarded scheme. The pipeline still cannot create, remove or reconfigure services.

## Consequences

- The deploy key now lives in GitHub instead of on the VPS. Mitigations: the key can only point an existing service to another `ghcr.io/pedrohrb7/` image; GitHub does not pass secrets to workflows triggered from forks, and this workflow only runs on pushes to `main`; the `production` environment only accepts deployments from `main`.
- The VPS address is in the workflow instead of a secret. It is not a secret: `portwallet.pedrohrb.dev` resolves to it.
- Builds use the GitHub Actions cache (`type=gha`, one scope per image), so unchanged layers are reused between runs. The VPS no longer builds anything.
- The stack and its secrets stay on the server (Portainer); the first deploy of a new service is still done there.
- Images and deploys follow the `app` path filter. A push that only touches docs, `*.md` files or `docker-compose.yml` deploys nothing, and a change to `.github/workflows/ci.yml` runs the whole pipeline, deploy included.
- The workflow's `concurrency` setting cancels an older run when a newer push arrives, which can stop a deploy halfway (for example, after the API and before the web). The newer run deploys both images, so this is accepted.
- There is no separate manual trigger, as Woodpecker had: redeploying a commit means re-running its workflow run.
- The other consequences of ADR 0009 still hold: the in-memory H2 database starts empty on every deploy, and the simulated inbox is public.
