# CareerConnect frontend

React, TypeScript, and Vite. Install dependencies with `npm ci`, then use
`npm run dev` from this directory. The local frontend runs on port 3000 and
uses `VITE_BACKEND_URL` or `http://localhost:8080` for the backend.

## Routes and integration

| Route | Feature |
| --- | --- |
| `/` | Welcome page and shared sign-in header |
| `/account/resume` | Resume management |
| `/account` | Profile editor from PR #64 |
| `/account/applications` | Application-status list from PR #70 |

The last two routes are added by their respective PRs; both are present after
combining those branches. The backend being run must include the corresponding
integration changes. Existing local Auth0 configuration is still required for
real sign-in. Frontend code contains no client secret.

`SessionProvider` checks `GET /api/v1/cc/security/me` with credentials. Signed-out,
unavailable, and loading states are handled separately. Fresh saved profile
values update the shared header greeting. Account authorization is enforced on
the server, including the profile and application-list routes.

## Profile editor (#64)

The profile loads and saves name/contact email at `/api/v1/users/{userId}`.
These database fields are separate from the provider's sign-in email. The backend
checks ownership and validation, and `/me` reads saved values after a reload.
The interface validates server confirmation rather than assuming any 2xx is success.

Auth0 database accounts can confirm and change their password through the separate
`PUT /api/v1/users/{userId}/password` route. The server enforces the 15-character
minimum; Auth0 may require additional complexity. Success invalidates the local
session. Google and other external-provider accounts use their provider's password
settings instead. Passwords are not stored in browser storage or the local database.

See `../docs/Sprint2-Issue32-Handoff.md` for the remaining live checks.

## Application statuses (#70)

The interface loads `GET /api/v1/account/applications` using the signed-in account.
It displays the four required labels, status filters/counts, refresh, and appropriate
loading, empty, session, and error states. Unknown statuses are shown neutrally.
Legacy SUBMITTED/Offer/Reject values are mapped at the server boundary. No sample
application records or guessed endpoints are used in production.

See `../docs/Sprint2-Issue44-Handoff.md` for the contract and live checks. The
recruiter's status-update workflow remains in separate PR #74.

## Validation

Run `npm run build` for TypeScript and a production build. Lint the changed feature
files with the commands in the integration package. Full `npm run lint` still
reports the existing effect error in `src/components/Resume.tsx` on this main base.

The package includes a reproducible browser harness using controlled API responses,
plus the result summary. The backend includes controller, service, HTTP-adapter,
and SQLite tests. These checks supplement the live sign-in and persistence checks;
they do not establish that a particular Auth0 tenant is configured correctly.

## Development notes

Shared document styles are in `src/styles/index.css`, shared shell/welcome styles
in `src/styles/App.css`, and feature styles in `Account.css` and `Applications.css`.
Stop Vite with Ctrl+C before running another server on port 3000. Keep local runtime
files, database changes, and configuration secrets out of feature commits.
