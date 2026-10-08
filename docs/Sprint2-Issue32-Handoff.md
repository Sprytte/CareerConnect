# Sprint 2, issue #32: profile editing interface

Prepared from uploaded repository snapshot
`0eed6e68beca0240dc11886d56cc1a4f21c4773a`, on 2026-10-08.

Issue: https://github.com/Sprytte/CareerConnect/issues/32

Branch: `feature/build-the-profile-editing-interface`

## Current status

The frontend implementation has been applied locally and is ready for a draft
PR. **Issue #32 is not fully complete:** live name saving is blocked by the
Google/Auth0 connection policy and backend error handling. Email and password
updates still need backend support and frontend integration.

The issue author allows either a new page or an edit mode on `/account`.
This implementation creates `/account` and uses the existing navy/teal UI.
On 2026-10-08, Émilie confirmed the frontend/backend split and Brendan confirmed
that he will handle the backend updates starting October 9. Arley handles the frontend. Émilie
stated October 26 as the Sprint 2 deadline. The email/password endpoint paths,
request bodies, success responses, and error behavior still need to be agreed
with Brendan before the frontend can integrate those updates.

## Requirements and implementation

| Requirement from the issue | Implemented behavior | Remaining work |
| --- | --- | --- |
| Editing page or edit mode on `/account` | New `/account` route and signed-in header link; page and live profile loading checked locally. | Complete remaining manual checks and review. |
| Change name | Loads latest profile, validates name, sends name-only PATCH, checks server response, refreshes greeting after confirmed success. | Resolve the confirmed Google/Auth0 connection blocker and backend error handling, then verify persistence and permissions. |
| Change email | Editable field with required/email-format validation. Changed email is blocked before sending a request. | Backend currently ignores email; add the agreed update behavior. |
| Change password and confirm it | New-password and confirmation fields, mismatch/required errors, visibility control, and clear action. | No password-update backend contract exists. The button checks the fields; it does not save. |
| Error management | Session unavailable/signed out, load retry, validation, 401, 403, network/HTTP failures, invalid responses, and unconfirmed updates. | Live integration with the backend's agreed error responses. |

## How the frontend works

1. `SessionProvider` asks `/security/me` whether the browser has a valid server
   session. The account form does not render while that check is pending, fails,
   or returns signed out.
2. `/account` gets the user ID from that verified session and loads
   `/security/user-info/{encodedUserId}`. This reads the current Auth0 profile
   instead of populating the form from potentially stale ID-token claims.
3. The form keeps a copy of the last server-confirmed profile and separate draft
   input values. Cancel discards drafts. It does not send a backend request.
4. On Save, the interface validates name and email. If the email differs, it
   reports that email updates are unavailable and sends no request. This also
   prevents saving a changed name while silently discarding a changed email.
5. A supported name-only edit sends `PATCH` with browser credentials and
   `{ "name": "Updated Name" }`. While saving, inputs and actions are disabled.
   A synchronous request guard prevents repeated submissions.
6. Success requires a usable profile response for the same user, containing the
   requested name and unchanged email. A 201 status by itself is insufficient.
   Verified values update the form and shared session greeting.
7. The password form validates required fields and equality without inventing
   an Auth0 password policy. It sends no password, stores none in local or
   session storage, clears the fields after checking a matching pair, and states
   that the password was not changed.
8. Profile loads and saves use AbortController cleanup so obsolete requests do
   not update an unmounted editor.

For example, changing the name alone can use the existing endpoint. Changing
name and email together is blocked before either edit is submitted because the
backend currently ignores the email. If a backend failure returns 201 with a
null body, the interface displays an error rather than a false success message.

## One supporting backend correction included

In `SecurityConfig.corsConfigurationSource()`:

```java
config.addAllowedMethod("PATCH");
```

The original code used `addAllowedHeader("PATCH")`. This is corrected so that
cross-origin name-update requests can use the existing PATCH endpoint. The
backend must be restarted after installation. No account-update business logic
or authentication configuration has otherwise been changed.

## Backend findings to coordinate with Brendan

| Source | Finding | Needed decision or correction |
| --- | --- | --- |
| `Auth0ManagementService.getPatchFormatterBodyUser` | Builds a body containing only name. `UserRequestModel.email` is not forwarded. | Brendan implements the agreed email update behavior; agree on Auth0 verification/connection behavior. |
| `UserRequestModel` and user-info PATCH | No user password field or password-update contract. | Brendan defines password changes and the behavior for provider-managed accounts. |
| `SecurityController` user-info GET/PATCH | Authentication/ownership checks are commented out. | Authenticate requests and derive or verify the target user against the server principal. The UI gate cannot enforce this. |
| `Auth0ManagementService.updateUserInfo` | Does not check Auth0's HTTP status before mapping the response; catches failures and can return null. Live testing confirmed an Auth0 400 was mapped into empty profile fields and returned as 201. | Return clear failure statuses and a trustworthy success response. |
| Google/Auth0 connection policy | Live name updates return 400 with `operation_not_supported` while profile attributes sync at each login. | Review the connection policy with the tenant owner and agree on editable profile behavior; do not change shared settings without team coordination. |
| `getPatchFormatterBodyUser` | Inserts name with string formatting instead of JSON serialization. | Serialize JSON correctly so names containing quotes or backslashes work. |
| `SecurityController.getCurrentUser` | `/me` reads the login principal's existing claims. | Decide how current session data reflects profile updates after a hard reload. |
| Auth0 PATCH and `UserServiceImpl` | Profile update changes Auth0; no corresponding local User-table update is shown. | Decide whether and how the local name/email should be kept in sync. |

These findings combine inspection of the supplied code with local authenticated
testing on October 8. They are not claims about any unseen deployment or branch.

The live request sent a name-only JSON body. Auth0 returned HTTP 400 with
`operation_not_supported`, explaining that the Google connection does not allow
updating `name` while profile attributes sync at each login. The local API still
returned 201 with this body:

```json
{"userId":"","email":"","name":"","picture":"","user_metadata":{}}
```

The frontend rejected that response because it could not confirm the current
user's profile. This confirms failure handling, not successful name persistence.
Auth0 documents connection sync behavior here:
https://auth0.com/docs/manage-users/user-accounts/user-profiles/configure-connection-sync-with-auth0
The appropriate connection policy remains a backend/team decision.

Before integration, obtain Brendan's endpoint paths, request/response examples,
and error statuses for email and password changes. Coordinate the included CORS
correction with his branch to avoid duplicate work. Backend ownership is settled;
the API contract and live verification remain pending.

## Validation performed

| Check | Result |
| --- | --- |
| `npm ci` against the supplied lockfile | Passed. |
| `npm run build` | Passed in package checks and Arley's Windows checkout: TypeScript and Vite production build. |
| ESLint on all changed TypeScript/TSX files | Passed in package checks and Arley's Windows checkout. |
| Project-wide `npm run lint` | Failed on the existing Resume.tsx set-state-in-effect error. |
| Original Resume.tsx checked using the same ESLint configuration | Same failure reproduced; the component was not changed. |
| Synthetic browser integration suite | 24/24 scenarios passed; results and harness included in the change package. |
| Desktop and mobile visual inspection | Completed; no clipped inputs or horizontal overflow at 1440, 760, 375, and 320-pixel widths in tested fixtures. |
| Java compilation | Passed locally using Gradle 8.4 and JDK 17. |
| Backend startup | Passed locally on port 8080. |
| Real Auth0 login and profile loading | Passed locally with Google sign-in. |
| Real Auth0 name persistence | Blocked: Auth0 400 `operation_not_supported`; local API returned 201 with empty profile fields, correctly rejected by the frontend. |

These results do not claim that backend tests, live email/password updates, or
every manual checklist item below have passed.

Browser scenarios cover signed-out/unavailable sessions, fresh profile loading,
validation and focus, cancel, request payload and cookie behavior, repeated
submission, unsupported email changes, HTTP/network failures, invalid 201
responses, wrong returned identity, ignored updates, expiry/permission errors,
retry, password confirmation, navigation, mobile/long-name layouts, and text
rendering. Preview email `arley@example.test` is synthetic fixture data.

## Local verification checklist

- [x] Apply the initial patch on the specified feature branch.
- [x] Start/restart the backend, then the frontend.
- [x] Sign in and open `http://localhost:3000/account`.
- [x] Verify that existing name/email load from the current profile.
- [x] Run the frontend production build and changed-file ESLint checks locally.
- [x] Compile Java locally using JDK 17.
- [ ] Inspect the final staged diff before committing.
- [ ] Change the name, save, reload `/account`, and confirm persistence after the backend fix. Currently blocked by the Google/Auth0 connection policy.
- [ ] Verify Cancel, invalid fields, and browser network errors.
- [ ] Confirm successful name updates and required team backend checks after Brendan's changes.
- [ ] Agree on Brendan's API contract and connect email/password updates before claiming #32 complete.
- [ ] Test email/password changes and server ownership checks after integration.

## Draft PR wording

Title: Add account profile interface and document update dependencies

Description:

> Adds `/account` with live profile loading, name-edit validation, cancel,
> server-response checks, session/error states, and responsive styling. Corrects
> the CORS configuration to permit PATCH.
>
> Live Google name saving is blocked by Auth0's connection sync policy. The
> backend maps Auth0's 400 error into empty profile fields and returns 201; the
> frontend rejects that response. Email and password updates await backend
> support and frontend integration. Brendan owns the backend follow-up.
>
> Refs #32. Keep this PR in draft and the issue open while integration is pending.
>
> Validation: frontend build and changed-file lint pass; 24 fixture-based browser
> scenarios pass. Java compilation, backend startup, and live profile loading
> passed locally. Full lint has the existing Resume.tsx error. Successful live
> name persistence and email/password integration remain pending.

Use `Refs #32`, not an automatic closing keyword, while required updates remain
unimplemented.
