# Plan: <ID> <Feature name>

- **Status:** Draft
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** <features done before this one>

## Steps

Each step leaves the build green and ends with the test that validates it.

### 1. <Short title>

- [ ] <task>
- **Files:** `<path>`
- **Validation:** <test or command that proves the step>

### 2. <...>

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] Feature E2E scenarios green in Chromium and Firefox
- [ ] Screen reviewed on desktop and mobile
- [ ] Spec updated with what changed during implementation
- [ ] User manual written or updated (features with a UI)
- [ ] READMEs (`README.md` and `README.pt-BR.md`) and the `docs/` index updated

## Risks

- <risk>: <mitigation>
