# 4. Money as a string in JSON

Date: 2026-10-30

## Status

Accepted

## Context

The backend handles money as `BigDecimal` with scale 2 and the database as `DECIMAL(19,2)`, as the PRD requires. The JSON `number`, read by JavaScript, is a 64-bit floating point: values like `0.1` have no exact representation, and values above 2^53 cents lose precision. The PRD's request example uses a number (`"amount": 150.00`).

## Decision

- Responses always write money values as a string with two decimal places (`"150.00"`), through the `Money` type and its serializer.
- Requests accept a string or a number. The number is read from its original text, without going through `double`, so `0.1` becomes exactly `0.10`.
- More than two decimal places is rejected with 400 `validation-error`, pointing at the field; there is never any rounding.
- The frontend keeps values as strings and only converts them for display with `Intl.NumberFormat`. No money arithmetic happens in the client.

## Consequences

- The PRD example stays valid, because a number is also accepted.
- Clients have to treat the value as a string, which is explicit in the API contract and in the conventions.
- Any DTO that exposes money uses `Money`, never `BigDecimal` or `double` directly, to guarantee the format.
