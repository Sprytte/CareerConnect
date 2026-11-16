# Sprint 3 AI Usage Log

Course: SOEN 341 - Software Process  
Project: CareerConnect - Job Search and Application Tracking Platform  
Sprint: Sprint 3  
Responsible Person: Brendan Kelly

## Task 1: Planning Application Editing and Withdrawal

Purpose of AI Use:  
I used the AI to choose where the ownership, deadline, and resume checks for
`PUT` and `DELETE /api/v1/account/applications/{applicationId}` should live and
which HTTP statuses the endpoints should return.

Chat Link or Prompt/Response:  
Prompt examples: "Where should ownership and deadline checks live in this Spring
Boot service?" "Which status codes should update and withdraw return for a
missing application, another user's application, or a passed deadline?"

AI-Suggested Content:  
The AI recommended keeping the authorization and deadline checks in
`AccountApplicationService` so both endpoints share the same rules, leaving the
controller responsible only for the session check. It suggested `401` for no
session, `403` for another user's application or resume, `404` for missing
records, `400` for a missing resume or passed deadline, and `204` for a
successful withdrawal.

Validation:  
I compared the suggestions with the existing application service patterns and
the error handling already used elsewhere in the backend. I then checked the
endpoints with the application tests and authenticated requests.

Decision:  
Modified before use. I kept the checks in the service but retained the existing
controller-level session handling and matched the project's existing response
style.

Reflection:  
The AI helped make the two endpoints behave consistently and clarified the
difference between "not found", "forbidden", and "no longer editable".

## Task 2: Designing the Role Model and SQLite Migration

Purpose of AI Use:  
I used the AI to plan how to add a role to an existing local user profile
without deleting the SQLite database, and how to represent the permissions for
each account type.

Chat Link or Prompt/Response:  
Prompt examples: "How should I add a nullable role column to an existing SQLite
database when Hibernate uses ddl-auto=none?" "How can I model job-seeker and
recruiter permissions in Java?"

AI-Suggested Content:  
The AI suggested a nullable `VARCHAR(32)` role column, a `UserRole` enum holding
the display name and permission strings, and a startup migration that checks
`sqlite_master` and `PRAGMA table_info(users)` before running `ALTER TABLE`.
It also suggested keeping `schema.sql` responsible for new databases.

Validation:  
I reviewed the migration against the project's SQLite setup and added a test
that creates the old users table, runs the migration, and runs it again to
confirm it is idempotent. I checked the role and permission fields in the
profile and session responses.

Decision:  
Accepted with small changes. I kept the local database as the source of truth
instead of copying roles from Auth0 claims.

Reflection:  
The AI helped avoid a destructive database reset and made the startup upgrade
safe for existing development databases.

## Task 3: Implementing Role Selection and Permission Enforcement

Purpose of AI Use:  
I used the AI to sketch the one-time role-selection endpoint and to find a
consistent way to enforce recruiter and job-seeker permissions across the
existing controllers.

Chat Link or Prompt/Response:  
Prompt examples: "Sketch a role-selection endpoint that only the account owner
can call." "How do I enforce recruiter and job-seeker access without repeating
the same check in every controller?"

AI-Suggested Content:  
The AI proposed `PUT /api/v1/users/{userId}/role`, with an owner check, enum
parsing for values such as `job-seeker`, an idempotent repeat of the same role,
and rejection of a role change after the first selection. It suggested a shared
`requireRole` service method and calls on the recruiter and job-seeker routes.
It also suggested returning the role and permissions from the profile and
session endpoints and redirecting role-less users to `/onboarding/role`.

Validation:  
I reviewed the controller and service flow, then covered first selection,
repeated selection, rejected second roles, another account, invalid roles, and
signed-out requests in `RoleSelectionTest`. `RoleEnforcementTest` confirms that
the server returns `403` when the frontend is bypassed.

Decision:  
Modified before use. I adjusted the error messages and kept the existing
authentication principal handling.

Reflection:  
The AI helped keep authorization on the server while exposing the same role to
the frontend for navigation.

## Task 4: Building the Onboarding Page and Role-Aware Navigation

Purpose of AI Use:  
I used the AI to structure the React onboarding page and to work through the
loading, signed-out, unavailable, saving, and error states.

Chat Link or Prompt/Response:  
Prompt examples: "Build a React page for choosing an account type with accessible
radio cards." "How should RoleGate and the header react when a signed-in user
has no role yet?"

AI-Suggested Content:  
The AI suggested a session-aware `RoleSelection` page with a single selected
role, an `AbortController` for the save request, an error summary that receives
focus, and a session update followed by navigation. It also suggested a
`RoleGate` redirect, role-aware header links, and a small role API helper.

Validation:  
I reviewed the generated structure, adjusted the copy and styling to match the
rest of the site, and checked the page in the browser at different window sizes.
The frontend production build passed after the changes.

Decision:  
Accepted with modifications. The final wording, layout, and accessibility
details were adjusted by hand.

Reflection:  
The AI was useful for the page structure and edge cases, while the visual and
interaction details still needed manual review.

## Task 5: Tests, Rebase, and Build Fixes

Purpose of AI Use:  
I used the AI to review the test cases for role selection and migration and to
diagnose the frontend TypeScript errors that appeared after rebasing the role
branch onto main.

Chat Link or Prompt/Response:  
Prompt examples: "What cases should a role-selection test cover?" "Why is the
TypeScript build failing after this rebase?"

AI-Suggested Content:  
The AI listed the role-selection and migration cases that became the backend
tests and pointed out places where the session user type needed to include the
new role and permissions. It also suggested a CSS fix to stop the onboarding
buttons from wrapping.

Validation:  
I ran the backend test suite and `npm run build`, then checked the role flows in
the browser. The final changes were included in PR #97.

Decision:  
Accepted. The build and tests passed after the fixes.

Reflection:  
The AI made the test matrix and the type errors easier to work through, but the
final verification came from running the project and reviewing the diff.
