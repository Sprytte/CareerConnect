# CareerConnect frontend

The welcome page uses React, TypeScript, and Vite with the project's existing dependencies.

## Local setup

Install Node.js 22.12+ and npm (the locked Vite version also supports Node.js 20.19+ within version 20). From the repository root in PowerShell:

```powershell
cd frontend
npm ci
npm run dev
```

Open the local URL printed by Vite. Stop the server with Ctrl+C.

## Checks and production preview

From the repository root:

```powershell
cd frontend
npm run lint
npm run build
npm run preview
```

The build checks TypeScript and produces the production assets. Open the URL printed by the preview server. There is currently no automated test script in `package.json`.

Check desktop and mobile layouts, navigation links, keyboard focus, and reduced-motion behavior.

## Sign-in integration

The header opens the backend's OAuth2 login route in the browser. The backend
handles Auth0 and redirects back to the frontend. No Auth0 client secret or
React Auth0 SDK belongs in this frontend. By default, the backend runs at
`http://localhost:8080`. To use another origin, set `VITE_BACKEND_URL` in a
local, untracked `.env.local` file in `frontend/` and restart Vite:

```text
VITE_BACKEND_URL=http://localhost:8080
```

The header also requests `GET /api/v1/cc/security/me` with browser credentials
after loading. **The backend does not implement this endpoint yet.** Agree on
and implement a server-validated current-user response before expecting the
header to display a signed-in account. The proposed response is:

```json
{"userId":"auth0|example","name":"Example User","roles":["USER"]}
```

Return 401 when signed out. The backend must derive user ID and roles from the
authenticated session, not a caller-supplied user ID or a readable cookie.
Cross-origin calls from `http://localhost:3000` must allow credentials. The
header reports account status unavailable while this endpoint is absent or
unreachable. The sign-out form posts to the backend's documented logout route;
confirm that the backend redirects to the frontend after logout.

Other API calls that need the session should use `credentials: 'include'`.
Role-based pages also need server-enforced authorization; hiding a link in React
does not protect backend routes.

## Current scope

- A responsive welcome page with reusable `SiteHeader` and `SiteFooter` components.
- In-page links to Features, How it works, and the top of the page.
- Static feature descriptions and a decorative account/resume preview. The preview is hidden from assistive technology and does not display account data or accept uploads.
- Sign-in opens the backend's OAuth2 route; the account display and sign-out
  depend on the backend contracts described above.

The welcome page still runs without a backend, but account status cannot load
without one. Resume management and protected role-based pages are separate
integrations.

`src/App.tsx` composes the page, `src/App.css` contains component and responsive styles, and `src/index.css` holds shared document defaults. Header and footer markup lives in `src/components`.
