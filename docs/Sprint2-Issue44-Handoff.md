# Sprint 2: application-status interface (issue #44)

Base: `d5696db` on main. Issue: “Build interface for users to view their
application statuses.” Parent story: Application Status Tracking #10.

## Scope and completion

The interface implements a new page at `/account/applications` and a reusable
status component with the four required labels: Applied, Interview, Offered,
and Rejected. Each badge combines text with a distinct color. The page also
supports status filters, counts, refresh, loading, empty, retry, sign-in,
permission, and unavailable states. It is read-only.

**Live application data is not connected.** The inspected base contains no
application entity, repository, controller, or application-status endpoint.
The production loader explicitly returns `unavailable`. It makes no application
request and does not substitute fixtures or invent an API URL. Keep the PR as
a draft and issue #44 open until the real data adapter and live checks below
are complete. Interface fixture checks do not establish backend integration.

## Files and behavior

- `frontend/src/components/Applications.tsx`: page, session gate, application
  list, filters, loading lifecycle, refresh, and user feedback.
- `frontend/src/components/ApplicationStatusBadge.tsx`: reusable text/color
  badge. Unknown or null statuses display neutral “Status unavailable”.
- `frontend/src/applications/applications.ts`: internal view model, result
  validation, known status labels, and calendar-date formatting.
- `frontend/src/applications/loadApplications.ts`: the data adapter boundary;
  currently returns unavailable.
- `frontend/src/styles/Applications.css`: responsive page and badge styles.
- `frontend/src/main.tsx`: route registration.
- `frontend/src/components/SiteHeader.tsx` and `frontend/src/styles/App.css`:
  signed-in Applications link, usable home links from subpages, and header
  layout at smaller widths.

Session data comes from the existing shared provider and backend `/me`
endpoint. A signed-out, unavailable, or still-loading session never starts
the applications loader. There is no client-side role assumption because the
current session response does not supply roles.

Application data is cleared on refresh, sign-out, or a change of account.
Outstanding loads are canceled on unmount or identity change, and obsolete
results are ignored even if an adapter resolves after cancellation. Refresh
preserves the selected filter. Counts are shown only after a validated ready
result. All includes unknown statuses; the four known-status filters include
only exact matches, so their counts need not sum to All.

## Internal data boundary

This model is for rendering and testing; it is **not an existing wire contract**.
An adapter takes an `AbortSignal` and resolves to one of:

| Result | Meaning |
| --- | --- |
| `{ status: 'ready', applications: JobApplication[] }` | A confirmed list for the authenticated account, including an empty array when there are no applications. |
| `{ status: 'unavailable' }` | Tracking is unavailable; do not claim an empty list. |
| `{ status: 'unauthenticated' }` | Sign-in is required again. |
| `{ status: 'forbidden' }` | The account cannot access this list. |
| A rejected promise or malformed result | Generic load error with retry; raw errors are not displayed. |

Each internal `JobApplication` has:

| Field | UI meaning |
| --- | --- |
| `id: string` | Stable, nonblank, unique application identifier. |
| `jobTitle: string` | Nonblank role title. |
| `company: string` | Nonblank employer name. |
| `status: string \| null` | One of the four known labels, or an unknown value rendered neutrally. |
| `appliedOn?: string \| null` | Optional calendar date in `YYYY-MM-DD` form. Invalid or missing dates display “Applied date unavailable”. |

Invalid lists, duplicate IDs, missing required fields, and invalid field types
produce a load error instead of a misleading empty state. Date-only values are
formatted in UTC so a calendar date does not shift with the browser time zone.
Record text is rendered through React without HTML injection.

## Connecting the backend

1. Confirm the actual application-list endpoint, response fields, status values,
   and error behavior with the backend owner. Do not infer a contract from the
   internal UI model.
2. Replace the unavailable implementation in `loadApplications.ts`. Send browser
   credentials, pass the `AbortSignal` to the request, check HTTP outcomes, and
   validate/map the response. A login redirect or HTML response must not be
   treated as a successful empty list.
3. Fetch all applications belonging to the authenticated account. If the API is
   paginated, implement the agreed paging behavior so the page and filter counts
   do not claim a partial page is the full application history.
4. Enforce authentication and ownership on the server using the session. A hidden
   header link or a caller-supplied user ID is not authorization. Verify another
   account cannot access these applications.
5. Map the backend's agreed statuses explicitly. Preserve unknown statuses as
   unavailable; never default an unrecognized value to Applied. Map date/time
   fields to the agreed calendar date rather than slicing an arbitrary timestamp.
6. Verify the live behavior below, update this document and the PR description,
   then move the PR out of draft when the requirements are satisfied.

## Verification

The production build and targeted ESLint checks pass. The accompanying local
review harness imports the real components into a temporary fixture entrypoint,
uses synthetic applications and mocked sessions, and removes that entrypoint
afterward. The production route is checked separately with the real unavailable
loader. No fixture entrypoint, sample application data, or test toggle is shipped
in the application source.

Coverage includes the four badges and filters, counts, empty/no-match states,
unknown statuses, date fallbacks, malformed results, error/retry, refresh,
session gating, expired/forbidden results, cancellation, account changes,
record text escaping, keyboard focus, badge text contrast, and responsive
layouts from 320px through 1440px. Screenshots containing application records
are fixture previews, not live integration evidence.

Full-project lint still reports the baseline `Resume.tsx` effect error. No
dependency changes or backend changes are included. The existing dependency
audit warning remains outside this interface change.

## Remaining live checks

- Sign in and confirm the backend returns only the current account's complete
  application list, including its empty state.
- Verify each required status using real persisted records.
- Change a status through the supported backend/recruiter workflow, refresh,
  and confirm the new value. Reload and sign in again to check persistence.
- Check session expiry, denied access, backend failure, and retry against the
  real endpoint; verify cross-account access is rejected server-side.
- Review desktop/mobile layout with real role titles and employer names.
- When PR #64 is merged, retain both the profile and Applications header links
  while resolving any shared-header changes. Do not discard either feature.
