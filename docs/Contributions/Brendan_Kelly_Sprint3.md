# Sprint 3 Individual Contribution Log

**Name:** Brendan Kelly  
**GitHub:** [BrendanKelly84213](https://github.com/BrendanKelly84213)  
**Course:** SOEN 341 – Software Process  
**Project:** [CareerConnect](https://github.com/Sprytte/CareerConnect)  
**Sprint:** 3

## Estimated time spent

The hours below are retrospective estimates of my active work, reconstructed from work sessions and repository activity. They are approximate rather than an exact timesheet.

| Activity | Estimated time | Related work |
| --- | ---: | --- |
| Application editing and withdrawal API | 6 hours | [Issue #53](https://github.com/Sprytte/CareerConnect/issues/53), [PR #81](https://github.com/Sprytte/CareerConnect/pull/81) |
| Account roles, permissions, and role onboarding | 12 hours | [Issue #62](https://github.com/Sprytte/CareerConnect/issues/62), [PR #97](https://github.com/Sprytte/CareerConnect/pull/97) |
| Testing, rebase/build fixes, and code review | 2 hours | [PR #97 checks](https://github.com/Sprytte/CareerConnect/pull/97/checks), [PR #105](https://github.com/Sprytte/CareerConnect/pull/105), [PR #100](https://github.com/Sprytte/CareerConnect/pull/100) |
| **Total** | **20 hours** | **Approximate total** |

## Editing and withdrawing applications

I implemented the authenticated application-management endpoints for [Issue #53](https://github.com/Sprytte/CareerConnect/issues/53):

- `PUT /api/v1/account/applications/{applicationId}` changes the resume attached to an existing application.
- `DELETE /api/v1/account/applications/{applicationId}` withdraws an application.

The service layer resolves the authenticated user's application and rejects another user's application with `403 Forbidden`. Updates also require the replacement resume to belong to the same user. Both operations check the job posting deadline before changing anything, so an application cannot be edited or withdrawn after the posting closes. Missing applications, postings, and resumes return `404 Not Found`, invalid requests return `400 Bad Request`, and unauthenticated requests return `401 Unauthorized`. Withdrawal returns `204 No Content`.

I kept the ownership, deadline, and resume checks in `AccountApplicationService` instead of duplicating them in the controller, and updated `docs/API.md` with the request body, response, and error behavior.

**Evidence:** [Issue #53](https://github.com/Sprytte/CareerConnect/issues/53), [merged PR #81](https://github.com/Sprytte/CareerConnect/pull/81).

## Account roles and role onboarding

For [Issue #62](https://github.com/Sprytte/CareerConnect/issues/62), I added account types to CareerConnect while keeping Auth0 responsible for sign-in and sign-up.

On the backend, I added a `UserRole` enum with `JOB_SEEKER` and `RECRUITER` values and their permission lists. The local user profile stores the role in a nullable `role` column. New databases get the column from `schema.sql`, while `UserRoleSchemaMigration` upgrades an existing SQLite database at startup by checking the table columns and adding the column only when it is missing. The migration is safe to run repeatedly.

I exposed the role and permissions through the profile and session responses and added `PUT /api/v1/users/{userId}/role` for the one-time selection. The endpoint only accepts the account owner, accepts common spellings such as `job-seeker`, treats repeating the same role as idempotent, and rejects changing a role once it has been set. `SecurityController` sends signed-in accounts without a role to `/onboarding/role`.

Role-specific routes now enforce permissions on the server. Recruiters manage job postings and applicant lists; job seekers manage applications and resumes. A request from the wrong role receives `403 Forbidden` even if the frontend is bypassed.

On the frontend, I built the role-selection page, added `RoleGate` to redirect signed-in accounts without a role back to onboarding, and made the header and workspace navigation role-aware. The page handles loading, signed-out, unavailable, saving, success, and error states, allows only one role choice, and moves focus to the error summary when a request fails. I also added the page styling and updated the frontend README.

**Evidence:** [Issue #62](https://github.com/Sprytte/CareerConnect/issues/62), [merged PR #97](https://github.com/Sprytte/CareerConnect/pull/97), [role onboarding documentation](https://github.com/Sprytte/CareerConnect/blob/main/docs/Sprint3-Issue62-Role-Onboarding.md).

## Testing and integration

- Added `RoleSelectionTest` for successful selection, repeat selection, rejected second roles, another account attempting the change, invalid roles, and signed-out requests.
- Added `RoleEnforcementTest` to confirm that recruiter-only and job-seeker-only routes reject the wrong role.
- Added `UserRoleSchemaMigrationTest` to verify the migration against an existing users table and to confirm that running it again has no effect.
- Updated `ProfileSignInTest` for the new onboarding redirect.
- Ran the backend test suite and the frontend production build, and manually checked role selection, role-aware navigation, and the edit/withdraw flows.
- Resolved rebase conflicts and fixed the frontend TypeScript build and a button-wrapping layout issue before the role branch was merged.

## Review and collaboration

I reviewed teammate work during the sprint, including [PR #105](https://github.com/Sprytte/CareerConnect/pull/105) for application-status notifications and [PR #100](https://github.com/Sprytte/CareerConnect/pull/100) for AI-assisted resume feedback, and approved [PR #80](https://github.com/Sprytte/CareerConnect/pull/80). I also coordinated the role model with the existing sign-in and profile behavior so that role selection did not create a second authentication flow.

Both assigned Sprint 3 issues were completed and merged through PRs #81 and #97.
