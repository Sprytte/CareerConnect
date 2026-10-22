# Sprint 2: application-status integration (issue #44)

Reviewed against main `bb27dd0` on October 21, 2026. PR #71 adds applying to a
posting and PR #68 adds recruiter application lists. A separate job-seeker list
was still missing; this change supplies it and connects the existing interface.

## Endpoint and data

`GET /api/v1/account/applications` requires a signed-in OIDC user. The controller
uses the authenticated subject, with no caller-selected account ID. It returns
the complete list of that user's applications, newest first, with `Cache-Control:
no-store`. No session returns 401. Other users' applications are excluded by the
repository query and are not sent to the browser.

Each record contains `id`, `jobTitle`, `company`, `status`, and optional
`appliedOn` (`YYYY-MM-DD`). The service fetches posting details in one batch.
Applications whose postings were deleted remain visible with a neutral title
and employer fallback. The response does not include resume contents or another
applicant's personal data.

New applications are stored as Applied. The read boundary also handles existing
values and the current PR #74 contract:

| Stored value | Required label |
| --- | --- |
| SUBMITTED, Applied | Applied |
| Interview | Interview |
| Offer, Offered | Offered |
| Reject, Rejected | Rejected |
| Unknown or null | Neutral Status unavailable in the interface |

The frontend calls this endpoint with credentials, cancellation, and no caching.
It validates the array and required fields, converts positive numeric IDs into
stable UI IDs, and rejects duplicate/malformed records. An HTTP error or invalid
response cannot become a false empty list. A confirmed empty array shows the
empty state. There is no production sample-data fallback.

## Interface

`/account/applications` retains the four text/color badges, filters and counts,
refresh, loading, empty/no-match, permission, expired-session, and retry states.
Refresh keeps the chosen filter and re-reads current server data. Account changes
or unmounts abort old requests and prevent stale results from replacing the page.

The package records browser, backend, and SQLite test results. A separate local
compatibility test combines PR #74 without merging it remotely: create an
application, update it as the posting owner, clear persistence state, and reload
it as the applicant. This checks Interview, Offer, and Reject mappings plus
isolation from another applicant. PR #74 remains a separate team-owned PR.

## Before marking #70 ready

1. Start the backend containing this endpoint. An older backend in the original
   folder will not provide it unless that folder includes this change.
2. Sign in and check a genuine empty account. Upload a disposable resume and
   submit a local test application through the existing apply API.
3. Verify it appears as Applied and remains after refresh and signing in again.
4. With the agreed recruiter status-update implementation available, change a
   record's status and refresh the applicant page. Check the four required labels.
5. Confirm a second applicant cannot see the first applicant's records.
6. Attach frontend screenshots as requested in the review. Identify any synthetic
   examples as fixtures; use real local data for integration evidence.

## Merge coordination

#64 and #70 share the header and router. Preserve both account links and both
routes. The package supplies tested merge reference files. PR #74 also conflicts
with main in the application service interface/implementation: preserve both
`getApplicationsByJobPostingId` and `updateApplication` when resolving it.
