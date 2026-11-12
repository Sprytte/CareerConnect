# Sprint 3: account creation with a role (issue #62)

## Decision

Keep Auth0's hosted Universal Login for sign-in and sign-up. A new account
chooses **Job seeker** or **Recruiter** in CareerConnect immediately after the
first successful sign-in.

This is the smallest reliable change for the current codebase:

- Auth0 already owns credentials, social providers, MFA, password reset, and
  breach detection. The frontend never sees a password.
- The backend already creates a local profile from the authenticated subject in
  `GET /api/v1/cc/security/redirect`, so it can pause there until a role exists.
- The same onboarding step works for email/password and social identities. A
  custom sign-up form or Auth0 custom signup fields only cover database
  connections, so Google/Microsoft users would still need a second flow.
- The frontend keeps a single **Sign in** entry point. Auth0's hosted page
  already offers its own sign-up option, so there is no duplicate app button to
  maintain or explain.

## Alternatives considered

| Option | Why it was not chosen |
| --- | --- |
| Customize Auth0's hosted sign-up page with role fields | Requires dashboard/page customization, is limited to database connections, and does not cover social sign-in. It also splits the source of truth for authorization. |
| Redirect Auth0's sign-up link to our role page | Auth0 does not expose a reliable "after sign-up, redirect to this app route" hook for Universal Login without custom actions/pages. The app is only in control after the callback. |
| Build a completely custom sign-in/sign-up page over Auth0 | Re-implements password, MFA, lockout, reset, and provider flows. High security and maintenance cost with little benefit for this project. |
| Create the Auth0 user from a custom form and assign a role first | Viable for email/password only and already partly present in `Auth0ManagementService`, but it still requires the app to collect passwords and does not help social users. |

## Flow

1. The user selects **Sign in** and continues with Auth0. New users choose
   Auth0's sign-up option on the hosted page.
2. Auth0 authenticates the user and returns through Spring Security.
3. `GET /api/v1/cc/security/redirect` creates the local profile if needed.
4. If the local profile has no role, the backend redirects to
   `/onboarding/role`.
5. The page calls `PUT /api/v1/users/{userId}/role` with `JOB_SEEKER` or
   `RECRUITER`.
6. The backend stores the role on the local user and returns it with the
   permissions granted by that role.
7. The frontend updates the shared session and opens the role-appropriate
   workspace. `RoleGate` also redirects any signed-in, role-less account back to
   onboarding, so navigating around the app cannot skip the step.

## Data and API

`users.role` is a nullable `VARCHAR(32)` column. `schema.sql` creates it for new
databases, and `UserRoleSchemaMigration` adds it to existing SQLite files at
startup.

`GET /api/v1/cc/security/me` and `GET /api/v1/users/{userId}` return:

```json
{
  "userId": "auth0|...",
  "name": "Example User",
  "email": "user@example.test",
  "pictureUrl": "...",
  "role": "JOB_SEEKER",
  "permissions": ["jobs:read", "applications:manage", "resumes:manage"]
}
```

`role` is `null` and `permissions` is `[]` until onboarding is complete.

## Permissions

| Role | Permissions |
| --- | --- |
| `JOB_SEEKER` | `jobs:read`, `applications:manage`, `resumes:manage` |
| `RECRUITER` | `jobs:read`, `job-postings:manage`, `applicants:read` |

The backend rejects role-specific routes with `403` for the wrong role. The
header and pages use the same role to avoid showing actions the server will
reject.

## Auth0 tenant notes

The basic flow needs no tenant dashboard change beyond the existing application
and callback configuration. Auth0's hosted page provides both sign-in and
sign-up.

If the team also wants the role in the Auth0 ID token or `roles` claim, add an
Auth0 Action that copies an `app_metadata.role` value into a namespaced claim.
The local database remains the source of truth for CareerConnect permissions;
`Auth0LoginService` already tolerates a missing `https://careercontact/roles`
claim. Assigning Auth0 roles themselves would require known role IDs (or
Management API role lookup) and is a separate follow-up.

## Tests

- `RoleSelectionTest` covers first selection, idempotent repeat, preventing a
  second role, another account trying to set the role, invalid roles, signed-out
  requests, and role-specific route rejection.
- `ProfileSignInTest` covers first sign-in redirecting to onboarding and
  returning users keeping their saved profile.
- `OAuth2LoginFlowTest` covers the hosted Auth0 authorization redirect.
- `npm run build` covers the TypeScript production build.

## Follow-ups

- Decide whether account types may ever change and, if so, implement an
  auditable admin/support path rather than a client toggle.
- Optionally sync the local role to Auth0 `app_metadata` and expose it as a
  custom claim for downstream services.
- Lock down the remaining legacy job-posting update/delete routes that predate
  this change; they still do not require an authenticated principal.
