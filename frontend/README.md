# CareerConnect frontend

React, TypeScript, Vite, React Router, and the project's existing CSS styles.

## Local setup

From the repository root in PowerShell:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. Run the Spring Boot backend on port 8080 in a
second terminal. Auth0 configuration stays on the backend. To override the
backend origin, set `VITE_BACKEND_URL` in an untracked `frontend/.env.local`
file and restart Vite.

## Routes

| Route | Purpose |
| --- | --- |
| `/` | Welcome page and sign-in navigation. |
| `/account` | Signed-in profile editor; name updates and account-update dependency handling. |
| `/account/resume` | Existing resume component. |

The shared header links to `/account` after the backend verifies the session.
The account page links to the existing resume page.

## Authentication and profile data

`SessionProvider` calls `GET /api/v1/cc/security/me` with
`credentials: 'include'`. This endpoint is implemented in `SecurityController`;
it returns `userId`, `name`, `email`, and `picture`, or 401 when signed out.
It does not return roles. The backend owns the Auth0 OAuth2 login flow.

After verifying the session, `/account` loads the latest Auth0 profile using
`GET /api/v1/cc/security/user-info/{userId}`. The user ID comes from `/me`,
not from editable input, a URL query, or a readable authentication cookie.
The path identifier is URL-encoded. Fresh profile data updates the shared
header greeting while the frontend is open.

`PATCH /api/v1/cc/security/user-info/{userId}` currently forwards **only name**
to Auth0.
The editor sends only `{ "name": "Updated Name" }` and checks the returned
identity, name, and email before displaying success. Empty or invalid success
responses, ignored updates, HTTP failures, and network failures show errors.
Cancel restores the last server-confirmed values. Repeated submissions are
blocked while a request is pending.

## Issue #32 dependencies

Live testing with Google sign-in confirmed that Auth0 rejects name changes with
HTTP 400 and `operation_not_supported` under the current connection sync policy.
The backend maps this error into empty profile fields and returns 201, which the
frontend correctly rejects. Successful name persistence needs the backend and
connection-policy fix described in `docs/Sprint2-Issue32-Handoff.md`.

The requested email and password changes are **not implemented by the supplied
backend**. A changed email is rejected before sending a PATCH, preventing a
partial name save. The password form checks required fields and matching
confirmation, but transmits no password and does not claim to update it.
These limitations are explicit in the UI. They must be resolved before #32
can be considered complete. See `docs/Sprint2-Issue32-Handoff.md`.

The patch also corrects `SecurityConfig` to allow PATCH as an HTTP method,
rather than adding a header named PATCH. Restart the backend after applying it.
Existing ownership checks on the profile endpoints and stale `/me` claims
remain backend handoff items; a frontend session gate is not server authorization.

## Checks

```powershell
npm run build
npm run lint
```

The production build and changed-file lint checks passed for this patch.
Project-wide lint still reports the pre-existing
`react-hooks/set-state-in-effect` error in `src/components/Resume.tsx`.
It was also reproduced against the original uploaded file.

Twenty-four browser scenarios passed with intercepted, synthetic account API
responses. The change package includes the harness and results. This verifies
frontend behavior; it does not establish that live Auth0 updates work. Local
Windows checks subsequently confirmed frontend build and changed-file lint, Java
compilation with JDK 17, backend startup, and authenticated profile loading. Name
persistence remains blocked as described above. Backend tests and live
email/password integration remain pending.

## Files

`src/components/Account.tsx` owns the account page and form states.
`src/profile/profileApi.ts` owns profile requests and response checks.
`src/styles/Account.css` contains account layout and responsive styles.
`src/auth/session.ts` and `SessionProvider.tsx` share authenticated user state.
