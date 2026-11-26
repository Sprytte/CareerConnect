# Sprint 4: saved jobs page (issue #47, task 9.3)

Built on main `08b1744`.

## Interface

- **New page** `/account/saved-jobs`, linked from the header ("Saved jobs",
  shown to job seeker accounts next to "Applications") and from the job search
  page. It lists the signed-in user's saved jobs,
  most recently saved first, with company, location, salary, deadline, the
  date saved and expandable details.
- **Save a job posting when viewing it**: every open posting on the job search
  page has a **Save job** toggle for signed-in job seekers. It shows **Saved** once
  saved; pressing it again removes the job.
- **Remove a saved job**: **Remove** on the saved jobs page. The card
  disappears only after the server confirms.
- Open postings link to **Apply now**; archived postings and postings past
  their deadline show "Applications closed". A posting deleted by the employer
  stays in the list, marked as no longer available, so it can still be removed.
- **Job seekers only**: recruiter accounts see no **Save job** buttons, and the
  saved jobs page points them to their postings instead.

## Backend

Tasks 9.1 and 9.2 (save and remove) were not on main, so this branch includes
a small implementation in `SavedJobManagement` so the page works end to end:

- `GET /api/v1/account/saved-jobs`
- `PUT /api/v1/account/saved-jobs/{jobId}`
- `DELETE /api/v1/account/saved-jobs/{jobId}`

All three derive the account from the session, require a job seeker account
(`UserService.requireRole`, like the other job seeker endpoints) and are
documented in `docs/API.md`. Saving an archived posting is refused with 400,
the same answer as applying to one, and the list marks archived postings with
`job.archived`. `schema.sql` gains a `saved_jobs` table, created on startup
with `CREATE TABLE IF NOT EXISTS`, so existing databases need no manual step.

If another implementation of 9.1/9.2 is merged instead, only
`frontend/src/savedJobs/savedJobsApi.ts` needs to change to match it.

## Error handling

| Situation | What the user sees |
| --- | --- |
| Signed out or session expired | Sign-in prompt or sign-in link |
| List fails to load | Error with "Try again" |
| Save or remove fails | Message; the saved state is left unchanged |
| Posting deleted before saving (404) | "No longer available" message |
| Posting archived before saving (400) | "This job posting is closed" message |
| Recruiter account (403) | "Saved jobs are for job seekers" |
| Backend without saved jobs (404/405) | "Coming soon" on the page; no Save buttons on job search |

## Tests

Backend tests run with the Gradle build in CI:

- `SavedJobServiceTest`: listing, saving, removing, idempotency, account
  scoping, archived postings, and the controller's session and role checks.
- `SavedJobRepositoryTest`: the `saved_jobs` table and its unique constraint.
- `SavedJobRoleTest`: end to end through Spring Security and the schema; a
  recruiter gets 403 on all three endpoints and a job seeker gets an empty list.

Acceptance tests are tracked in the acceptance criteria issue linked to #47.
