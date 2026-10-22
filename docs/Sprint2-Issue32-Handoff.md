# Sprint 2: profile editing integration (issue #32)

Reviewed against main `bb27dd0` on October 21, 2026. Profile backend PR #72 is
merged. This change connects the interface to that persisted profile and adds
the missing authenticated password-change path.

## Behavior

- `/account` loads `GET /api/v1/users/{userId}` and saves name and contact email
  together with `PUT /api/v1/users/{userId}`. Both require the same signed-in
  OIDC subject as the path. A different account receives 403; no session receives 401.
- The profile is stored in CareerConnect's SQLite database. Contact email changes
  do not change the email used to sign in at Auth0 or Google. The form explains this.
- `/api/v1/cc/security/me` reads the saved database values. Reloading or signing
  in again does not replace the edited profile with old token claims.
- Save confirmation requires the same account ID and both requested field values.
  Invalid responses never produce success. Duplicate contact email returns 409.
- Auth0 database accounts use `PUT /api/v1/users/{userId}/password` with
  `{ "password": "..." }`. The server enforces 15 characters, calls Auth0's
  Management API, checks its status and returned account ID, and responds 204
  only after confirmation. It invalidates the current local session afterward.
- Social/enterprise identities use their sign-in provider's password settings.
  The interface does not collect a Google password. The server also rejects a
  direct password request for those identities.
- Auth0's configured policy may require more than the local minimum. Policy and
  service failures are displayed without exposing response bodies, passwords,
  or tokens. Passwords are not placed in browser storage or database records.
- The general profile PUT rejects a password field instead of silently ignoring it.
- Legacy Auth0 profile read/write/delete routes now enforce ownership. Legacy
  employee administration requires the server-verified Admin claim. Cookie-free
  logout no longer throws a null-cookie exception.

## Validation and remaining checks

The package's review report records the build, browser, and backend test results.
The combined browser checks use controlled API responses. Backend controller,
service, HTTP-adapter, and SQLite tests are separate from live tenant verification.
The context smoke test mocks external identity discovery and uses an in-memory
SQLite database so it does not require live credentials or change the local database.

Before moving #64 out of draft locally verify:

1. Google sign-in, load the profile, save a new display name and contact email,
   reload, then sign out and in again. Confirm the saved database values persist.
2. Try an invalid email and duplicate contact email; confirm appropriate feedback.
3. Use a disposable Auth0 email/password account to test length, confirmation,
   successful password change, local session expiry, and login with the new password.
   The tenant client must have the Management API permissions needed to update users.
4. Test a second account and verify it cannot read or change the first account's
   profile or password. The automated controller tests cover these boundaries too.
5. Review desktop/mobile screenshots and updated CI results. New commits may need
   renewed review even though the earlier draft was approved.

## First sign-in and duplicate contact emails

The local schema requires unique contact emails. Google and email/password
sign-ins can represent different identities even when the email text matches.
If the new identity cannot create its local profile, the login redirect clears
the local session and returns to the welcome page with recovery instructions.
It never assigns the existing profile to the new identity by matching email.
The recovery button signs out through Auth0 so another sign-in method can be
chosen. Use a different email for a separate disposable password-test account.

`ProfileSignInTest` uses the actual SQLite initialization schema, including its
email constraint, to check this rejection, separate-account creation, the
preservation of edited details on a returning account, and duplicate-email
updates returning 409 without changing either profile. Constraint handling
checks SQLite's unique-violation code through exception causes, including
Hibernate's `JpaSystemException` wrapper. Unrelated persistence errors propagate.

## Merge coordination

#64 and #70 share `SiteHeader.tsx` and `main.tsx`. Keep both account links and both
routes. The package supplies tested merge reference files and a local integration
worktree script. Backend status-update PR #74 is not part of the profile change.

The review also identified pre-existing repository security issues outside this
interface. See the package's review report for the committed credential and the
remaining job-posting/resume authorization follow-up.
