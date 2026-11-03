# Spec: FR-17 Recipient lookup

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-17; Advanced security (lookup trade-off)
- **Depends on:** [fr-01-registration](../fr-01-registration/spec.md), [nfr-ledger-concurrency](../nfr-ledger-concurrency/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Let the user check who they are transferring to before confirming: they enter an e-mail or CPF and see the recipient's masked name, or pick one of the recent recipients.

## Scope

**Includes**

- `GET /recipients/lookup` and `GET /recipients/recent`.
- First step of the `/transfer` screen.

**Later**

- Dedicated rate limit (20 per minute and 200 per day per user): phase 5.
- Audit record of each lookup: phase 5.

The two items above are part of containing the enumeration trade-off described in the PRD. Until phase 5, the lookup requires authentication but has no limit.

## Behavior

1. The query accepts an e-mail or a CPF in the same field: with `@`, it is treated as an e-mail (normalized to lower case); otherwise, as a CPF (masked or not). An invalid format for either: 400 `validation-error`.
2. The CPF lookup uses the blind index (`document_hash`); the CPF is never decrypted to compare.
3. When an `ACTIVE` user other than the requester is found, the response carries the `recipientId` (ID of the target account) and the masked name.
4. Unknown, `PENDING` or `BLOCKED` user: 404 `recipient-not-found`, with the same message, "Destinatário não encontrado ou indisponível" ("Recipient not found or unavailable").
5. Looking up one's own e-mail or CPF: 422 `self-transfer`.
6. Name mask: full first name, followed by the initial of the last surname and four asterisks ("Ana Souza Lima" becomes "Ana L\*\*\*\*"). A one-word name shows in full.
7. `GET /recipients/recent` returns up to 5 distinct recipients of the `COMPLETED` transfers sent by the user, newest first, only those still `ACTIVE`.

## API

### `GET /api/v1/recipients/lookup?query=ana@demo.com`

- **Access:** USER

Response `200`:

```json
{ "recipientId": "6a1d...", "maskedName": "Ana S****" }
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Rule 1 |
| 404 | `recipient-not-found` | Rule 4 |
| 422 | `self-transfer` | Rule 5 |

### `GET /api/v1/recipients/recent`

- **Access:** USER

Response `200`:

```json
[{ "recipientId": "6a1d...", "maskedName": "Ana S****", "lastTransferAt": "2026-10-30T15:00:00Z" }]
```

## Data model

No migration. The lookup uses the unique indexes on `users.email` and `users.document_hash`; recent recipients use `ix_transactions_source_created`.

## Backend

- `account.application.RecipientService`: `lookup(UUID requesterId, String query)` and `recent(UUID requesterId)`.
- `shared.privacy.NameMasker`, already created by the [password on activation](../../backlog/refactor/2026-10-30-password-on-activation.md) refactor ([ADR 0005](../../adr/0005-password-set-on-activation.md)).
- `account.api.RecipientController`.

## Frontend

- `src/features/transfer/RecipientStep.tsx`: "E-mail ou CPF" ("E-mail or CPF") field (`data-testid="recipient-query"`), "Buscar" ("Search") button, result card with the masked name (`data-testid="recipient-result"`) and a "Continuar" ("Continue") button; clickable list of recent recipients (`data-testid="recent-recipients"`).
- Errors by `type`: not found, invalid format, own account.

## Security

- The `recipientId` is the account ID, not the user ID, and only serves as a transfer destination.
- The response never carries e-mail, CPF or full name.
- Logs record the lookup with a masked e-mail and the CPF omitted.

## Tests

| Layer | Scenario |
| --- | --- |
| Unit | `NameMasker` with one, two and three words and extra spaces (done) |
| Integration | Lookup by e-mail and by CPF, masked and unmasked; unknown, `PENDING` and `BLOCKED` return an identical body; own account gives 422; recent recipients distinct, ordered, at most 5 and without blocked recipients |
| Unit (frontend) | `RecipientStep` shows the masked name and the error messages |
| E2E | Part of PRD scenario 3: the lookup shows the masked name |

## Acceptance criteria

- [ ] Lookup by e-mail and CPF with a masked response
- [ ] Same response for unknown and inactive
- [ ] Recent recipients working

## Decisions

- **`recipientId` as the account ID:** the transfer references exactly the destination the user checked, without a new e-mail lookup that could change its result.

## Open questions

- None.
