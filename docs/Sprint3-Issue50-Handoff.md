# Sprint 3: view and filter applications (issue #50, task 11.3)

Built on main `90b1c03`. Frontend only: it uses the recruiter endpoints that
already exist, `GET /api/v1/account/job-postings` and
`GET /api/v1/account/job-postings/{jobId}/applications`.

## What was added

A recruiter page at `/account/jobs/applications` ("Applications received")
that lists the signed-in user's job postings with the applications received on
each. It is linked from **My postings** (top link, and a **View applications**
button on every posting, which opens the page narrowed to that posting).

## Filter tab

| Control | Options |
| --- | --- |
| Posting status | All, Open, Closed (a posting is open until its deadline) |
| Application status | All, Applied, Interview, Offered, Rejected |
| Date to filter | Date posted, Deadline, Date applied |
| From / To | Inclusive calendar dates; either end may be left empty |

When a filter is about applications (status, or date applied), postings with
no matching application are hidden and each card says how many of its
applications are shown.

## Sort tab

- Job postings: date posted (newest or oldest first), deadline (soonest or
  latest first), status (open or closed first), most applications first.
- Applications inside a posting: date applied (newest or oldest first), or
  status in the order Applied, Interview, Offered, Rejected.

## Error handling

| Situation | What the user sees |
| --- | --- |
| Signed out or session expired | Sign-in prompt |
| Postings fail to load | Error with "Try again" |
| One posting's applications fail to load | Error on that card only, with its own "Try again" |
| "From" is after "To" | Inline message; the date filter is not applied until fixed |
| No postings yet | Empty state with a link to create a job |
| Filters match nothing | Empty state with "Clear filters" |

Stored statuses are shown with the same labels the job seeker sees
(`SUBMITTED`/`Applied` as Applied, `Offer` as Offered, `Reject` as Rejected).

## Known limits

- The applications endpoint returns the applicant's account ID and resume ID
  but no name, email or resume file, so rows are labelled "Application #id".
  Showing who applied needs a backend change.
- Updating an applicant's status (Interview, Offer, Reject) is not part of
  this issue since its scope was changed to viewing and filtering.
- Applications are loaded with one request per posting.

## Tests

The filter and sort rules are covered by
`frontend/tests/postingApplicationsView.test.ts`: status mapping, open and
closed postings, each filter, inclusive date bounds, the invalid date range
and every sort option. They use Node's built-in test runner, so no packages
are added:

```powershell
cd frontend
npm test        # Node 22.6 or newer
```

In CI they run in the `frontend-tests` job of `.github/workflows/gradle.yml`.

Acceptance tests for the user story are in
`docs/Acceptance Tests/US-11-View-applicants.md`.
