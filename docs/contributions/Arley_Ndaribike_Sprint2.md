# Sprint 2 Individual Contribution Log

**Name:** Arley Ndaribike  
**GitHub:** [Arley100](https://github.com/Arley100)  
**Course:** SOEN 341  
**Project:** [CareerConnect](https://github.com/Sprytte/CareerConnect)  
**Sprint:** 2

## Estimated time spent

The hours below are retrospective estimates of my active work, reconstructed from work sessions and repository activity. They are approximate rather than an exact timesheet.

| Activity | Estimated time | Related work |
| --- | ---: | --- |
| Profile editing interface and API integration | 6 hours | [Issue #32](https://github.com/Sprytte/CareerConnect/issues/32), [PR #64](https://github.com/Sprytte/CareerConnect/pull/64) |
| Application-status interface and data integration | 4 hours | [Issue #44](https://github.com/Sprytte/CareerConnect/issues/44), [PR #70](https://github.com/Sprytte/CareerConnect/pull/70) |
| Integration debugging, regression tests, and final verification | 3 hours | [PR #64 changes](https://github.com/Sprytte/CareerConnect/pull/64/files), [PR #70 changes](https://github.com/Sprytte/CareerConnect/pull/70/files) |
| Code review, team coordination, and documentation | 2 hours | [My review of PR #69](https://github.com/Sprytte/CareerConnect/pull/69#pullrequestreview-5623487808), handoff documents linked below |
| **Total** | **15 hours** | **Approximate total** |

## Profile editing interface

I implemented the account profile editing page, including name and contact-email fields, password confirmation, validation, loading states, and success/error feedback. I connected the page to the backend, updated shared session information after profile changes, and addressed review feedback.

I integrated the completed backend behavior so that contact-email updates persist in the application database and password changes work for Auth0 email/password accounts. The contact email remains separate from the Auth0 sign-in email; social-account passwords remain managed by the sign-in provider.

**Evidence:** [Issue #32](https://github.com/Sprytte/CareerConnect/issues/32), [merged PR #64](https://github.com/Sprytte/CareerConnect/pull/64), [profile handoff documentation](https://github.com/Sprytte/CareerConnect/blob/main/docs/Sprint2-Issue32-Handoff.md).

## Application-status interface

I implemented the applications page and reusable status badges for Applied, Interview, Offered, and Rejected. I added status filtering, counts, refresh, and feedback for loading, empty, unavailable, and error states.

I initially kept the work in a draft PR while the required backend was being completed. I then connected the interface to account application data, resolved integration conflicts with the latest main branch, and completed verification before merging.

**Evidence:** [Issue #44](https://github.com/Sprytte/CareerConnect/issues/44), [merged PR #70](https://github.com/Sprytte/CareerConnect/pull/70), [application-status handoff documentation](https://github.com/Sprytte/CareerConnect/blob/main/docs/Sprint2-Issue44-Handoff.md).

## Testing and integration fixes

- Added regression tests for OAuth sign-in failure recovery and profile initialization during sign-in.
- Fixed handling of duplicate-email database conflicts and added coverage for the conflict behavior.
- Ran backend Gradle tests, frontend production builds, and targeted frontend lint checks during integration.
- Manually checked profile persistence and verified that signing in with a newly changed password worked.
- Checked application-status rendering, filtering, and empty-state behavior, including controlled fixture previews.

These fixes and tests are included in [PR #64](https://github.com/Sprytte/CareerConnect/pull/64) and the final integration work in [PR #70](https://github.com/Sprytte/CareerConnect/pull/70).

## Review and collaboration

I coordinated frontend/backend dependencies with teammates, responded to feedback on my PRs, and maintained the handoff documents. I also reviewed [PR #69](https://github.com/Sprytte/CareerConnect/pull/69), leaving comments on failed-request feedback, filename validation, and editing behavior during a pending save.

Both assigned frontend features were merged through PRs #64 and #70.
