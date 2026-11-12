# Sprint 3: edit and withdraw an application (issue #54, task 14.2)

Built on main `90b1c03`. The backend write endpoints from task 14.1
(`PUT` and `DELETE /api/v1/account/applications/{applicationId}`) already
existed; this change adds the interface for them on `/account/applications`.

## Behaviour

- Each application card offers **Edit application** and **Withdraw** while the
  job posting is open, and shows until when changes are allowed.
- **Edit** changes the resume attached to the application: pick another
  uploaded resume, or upload a new PDF/DOCX (up to 5 MB), which is saved to the
  account and attached. Save stays disabled until something actually changes.
- **Withdraw** asks for confirmation in the card, with focus on "Keep
  application". The card is removed only after the server confirms.
- After the deadline, or when the posting was deleted, the actions are replaced
  by a sentence explaining why the application can no longer be changed.
- After a saved edit or a withdrawal the list is re-read from the server and a
  confirmation is shown above it.

## Deadline rule

`GET /api/v1/account/applications` now also returns `resumeId`, `deadline` and
`modifiable` for each application (see `docs/API.md`). `modifiable` is decided
by the server clock with the same rule the write endpoints enforce. The
interface uses it to hide the actions; the server still rejects a late request
with 400, and the card then locks itself. With a backend that does not send
these fields the actions stay visible and the server check alone decides.

## Error handling

| Situation | What the user sees |
| --- | --- |
| Deadline passed (400) | Explanation; the card locks |
| Session expired (401) | Message with a sign-in link |
| Not the owner (403) | Permission message |
| Application or posting gone (404) | Asked to refresh the list |
| Server or network failure | Retry message; nothing is removed locally |
| Wrong file type, empty or oversized file | Inline message, Save disabled |
| Resume list fails to load | Message with "Try again" |

## Tests

Backend unit tests are in `AccountApplicationChangeTest` and run with the
Gradle build in CI. They cover the new list fields, editing, withdrawing,
the deadline rule and ownership.

Acceptance tests for the user story are in
`docs/Acceptance Tests/US-14-Edit-or-withdraw-application.md`.

## Not included

Editing anything other than the attached resume: the backend edit endpoint
only accepts `resumeId`.
