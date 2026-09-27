# CareerConnect frontend

CareerConnect uses React, TypeScript, Vite, and React Router. The frontend
includes a welcome page, shared account controls, and a resume upload page.

## Local setup

Use Node.js 22.12+ and npm.

From the repository root in PowerShell:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000/`. Stop the server with Ctrl+C.

Run the backend separately when testing authentication or resume uploads.
Its default address is `http://localhost:8080`.

## Checks and production preview

From the repository root:

```powershell
cd frontend
npm run lint
npm run build
npm run preview
```

The build checks TypeScript and produces the production assets. Open the URL
printed by the preview server. There is currently no automated test script
in `package.json`.

Use the development server on port 3000 for backend integration testing.
The preview server uses a different origin, which the backend and Auth0
configuration must explicitly support before authentication can be tested
there.

## Pages and navigation

| Route | Purpose |
| --- | --- |
| `/` | Welcome page with account controls and feature information |
| `/account/resume` | Resume page with session-based access handling |

The header links to the welcome page's Features and How it works sections,
and to My resume. Navigation remains available on smaller screens.

Both pages use the shared `SiteHeader` and `SiteFooter` components.

The welcome page's decorative account/resume preview is static. It is hidden
from assistive technology and does not display account data or accept uploads.

## Backend configuration

`src/constants.ts` defines the backend URL used by session checks and resume
uploads.

To use another backend origin, create a local, untracked `.env.local` file
inside `frontend/`:

```text
VITE_BACKEND_URL=http://localhost:8080
```

Restart Vite after changing this file.

Variables prefixed with `VITE_` are exposed to the browser. Keep Auth0 client
secrets and other private credentials on the backend.

## Authentication integration

Sign in opens the backend's OAuth2 route through normal browser navigation:

```text
GET /oauth2/authorization/okta
```

The intended flow is:

1. The backend redirects the browser to Auth0.
2. The user signs in or registers through Auth0.
3. The backend processes the callback and returns the browser to the frontend.
4. The frontend checks the authenticated session.

The complete registration, login, return-redirect, and logout flow still
requires integration testing. This frontend uses the backend-managed flow
and does not use the Auth0 React SDK.

### Shared session state

`src/auth/SessionProvider.tsx` is mounted in `src/main.tsx`. It loads the
session and shares the result with components through `useSession()` from
`src/auth/session.ts`.

The current implementation requests:

```text
GET /api/v1/cc/security/me
```

The request includes `credentials: 'include'`.

**This endpoint was not implemented in the backend version used for this
integration. Its URL and response contract remain provisional.**

The frontend currently expects:

```json
{
  "userId": "auth0|example",
  "name": "Example User",
  "roles": ["USER"]
}
```

All three fields are required by the current response validator. Update the
type and validator together when the backend contract is confirmed.

The backend must identify the user from the authenticated session and return
401 when signed out. Client-supplied identity values or readable cookies are
not proof of authentication.

Cross-origin session requests from `http://localhost:3000` must be allowed
with credentials by the backend.

### Account display and logout

The header displays the account name and Sign out when the session check
succeeds. Long names are visually truncated; the choice of display-name
field remains pending the backend contract.

Sign out submits a browser form to:

```text
POST /api/v1/careercontact/logout
```

The form allows the browser to follow the backend's logout redirects.
Session invalidation and the return to the frontend still need to be
verified against the working backend.

## Resume-page access

`src/ResumePage.tsx` wraps the upload component with shared page structure
and `src/auth/RequireAuth.tsx`.

The access component handles four states:

| Session state | Page behavior |
| --- | --- |
| Loading | Shows a session-check message |
| Signed out | Shows a sign-in prompt |
| Signed in | Renders the resume upload component |
| Unavailable | Shows an account-check error and a retry button |

A missing endpoint, an unexpected response, or a network failure produces
the unavailable state. It does not establish that the user is signed out.

This frontend check controls what the page displays. The backend must
independently enforce authentication and resume ownership.

## Resume-upload integration

`src/Resume.tsx` submits a multipart request containing a `file` field:

```text
POST /api/v1/resumes/users/{userId}
```

The request already includes `credentials: 'include'`.

The current picker accepts PDF files. The resume backend implementation
also supports DOCX files and a 5 MB file-size limit.

**Uploads still use `TEMP_USER_ID` in the current frontend implementation.**
Replacing it depends on the agreed user identifier and backend route.
Coordinate that change with the resume frontend and backend owners.

The current page provides upload functionality. Listing, downloading, and
deleting resumes are not implemented in this UI.

## Manual verification

Check the following before marking the integration complete:

- Welcome-page layout, keyboard navigation, and reduced-motion behavior.
- Header layout with a long display name.
- Navigation at desktop and mobile widths.
- Links from the resume page back to the welcome page and its sections.
- Loading, signed-out, signed-in, and unavailable account states.
- Registration and login through Auth0, followed by a return to React.
- Session recognition after refreshing the page.
- Access to the resume page after signing in.
- Upload using the correct authenticated user.
- Logout followed by a signed-out session check.
- Direct visits to `/account/resume` while signed out.

Development fixtures can help inspect UI states, but they do not verify
authentication or backend integration. Remove temporary preview fixtures
before committing application changes.

## Main files

| File | Responsibility |
| --- | --- |
| `src/main.tsx` | Application entry point, session provider, and routes |
| `src/App.tsx` | Welcome-page content |
| `src/ResumePage.tsx` | Resume-page structure and access wrapper |
| `src/Resume.tsx` | File selection and upload |
| `src/auth/session.ts` | Session types, response validation, request, and shared hook |
| `src/auth/SessionProvider.tsx` | Session loading and shared state |
| `src/auth/RequireAuth.tsx` | Resume-page access states |
| `src/constants.ts` | Shared backend URL |
| `src/components/SiteHeader.tsx` | Navigation and account controls |
| `src/components/SiteFooter.tsx` | Shared footer |
| `src/App.css` | Welcome, header, access-message, and responsive styles |
| `src/Resume.css` | Resume-upload styles |
| `src/index.css` | Shared document defaults |