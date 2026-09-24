# CareerConnect frontend

The welcome page uses React, TypeScript, and Vite with the project's existing dependencies.

## Local setup

Install Node.js 22.12+ and npm (the locked Vite version also supports Node.js 20.19+ within version 20). From PowerShell:

```powershell
Set-Location C:\Users\ndari\CareerConnect\frontend
npm ci
npm run dev
```

Open the local URL printed by Vite. Stop the server with Ctrl+C.

## Checks and production preview

```powershell
Set-Location C:\Users\ndari\CareerConnect\frontend
npm run lint
npm run build
npm run preview
```

The build checks TypeScript and produces the production assets. Open the URL printed by the preview server. There is currently no automated test script in `package.json`.

Check desktop and mobile layouts, navigation links, keyboard focus, and reduced-motion behavior. Sign in should remain disabled.

## Current scope

- A responsive welcome page with reusable `SiteHeader` and `SiteFooter` components.
- In-page links to Features, How it works, and the top of the page.
- Static feature descriptions and a decorative account/resume preview. The preview is hidden from assistive technology and does not display account data or accept uploads.
- Sign-in remains disabled pending the team's authentication decision. Authentication routing and integration will be connected once that flow is agreed.

This page does not implement authentication, resume management, or backend integration. It does not require a backend service, API keys, or environment variables to run.

`src/App.tsx` composes the page, `src/App.css` contains component and responsive styles, and `src/index.css` holds shared document defaults. Header and footer markup lives in `src/components`.
