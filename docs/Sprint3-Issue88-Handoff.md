# Sprint 3: View and update applicants (issue #88)

## Overview

This issue introduces a recruiter-facing interface for reviewing
applications submitted to a job posting and updating their status.

The implementation reuses the existing authentication session,
job API utilities, site layout, and job-posting navigation.

## Interface

The new JobApplicantsPage provides:

- Navigation from the recruiter's job postings.
- Job title, company, location, and application count.
- A selectable list of applications and their current statuses.
- Application details: applicant ID, application date, resume ID,
  and current status.
- Status selection: Interview, Offer, or Reject.
- Confirmation and error messages after update requests.
- Loading, empty-list, invalid-job, and session-related states.
- Disabled controls during updates to prevent duplicate submissions.

## API integration

The page uses the existing job API request utility.

GET /api/v1/account/job-postings/{jobId}
Retrieves the job posting details.

GET /api/v1/account/job-postings/{jobId}/applications
Retrieves applications associated with the posting.

PUT /api/v1/account/job-postings/{jobId}/applications
Submits an application status update.

Request body:
{
  "applicationId": 1,
  "status": "Interview"
}

The interface updates its local application list using the
application returned by the backend.

## Automated tests

The applicantStatus helper module contains reusable application
status logic.

Thirteen unit tests cover:

- Valid and invalid job posting IDs.
- Default status selection.
- Preserving an existing decision.
- Preventing invalid or duplicate updates.
- Updating the correct application.
- Preserving array immutability.
- Handling missing application IDs.
- Error message handling.

Validation performed:

- npm test: 13 tests passed.
- Targeted ESLint: passed.
- TypeScript node configuration check: passed.
- git diff --cached --check: passed.

The global frontend build was attempted but did not complete
because of three unused userId declarations in ApplyJobPage.tsx
and Resume.tsx, outside the files changed for issue #88.

## Manual acceptance scenarios

The following scenarios still require end-to-end verification
with a running backend and authenticated recruiter account.

1. Open a recruiter's job posting and navigate to applicants.
2. Verify the correct posting and application list are displayed.
3. Select an application and inspect its details.
4. Change its status to Interview, Offer, or Reject.
5. Verify the success message and updated status.
6. Refresh and confirm the backend persisted the change.
7. Verify empty, invalid-job, and unauthenticated states.
8. Verify API errors display an error rather than false success.
9. Confirm access is restricted to authorized recruiters.

## Integration notes

The implementation uses the existing application endpoints.
Backend compatibility and authorization must be verified
during integration testing.

Coordinate router and job-page changes with other team
members before merging overlapping pull requests.

Local database changes and uploaded test files are not part
of this issue and must not be committed.
