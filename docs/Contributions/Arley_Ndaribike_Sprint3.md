# Sprint 3 Individual Contribution Log

**Name:** Arley Ndaribike  
**GitHub:** [Arley100](https://github.com/Arley100)  
**Course:** SOEN 341  
**Project:** [CareerConnect](https://github.com/Sprytte/CareerConnect)  
**Sprint:** 3

## Estimated time spent

The hours below are retrospective estimates of my active work, reconstructed from work sessions and repository activity. They are approximate rather than an exact timesheet.

| Activity | Estimated time | Related work |
| --- | ---: | --- |
| AI-assisted resume feedback (frontend, backend and Gemini integration) | 10 hours | [Issue #59](https://github.com/Sprytte/CareerConnect/issues/59), [PR #100](https://github.com/Sprytte/CareerConnect/pull/100) |
| Editing and withdrawing applications | 6 hours | [Issue #54](https://github.com/Sprytte/CareerConnect/issues/54), [PR #93](https://github.com/Sprytte/CareerConnect/pull/93) |
| Recruiter view to filter and sort applications | 6 hours | [Issue #50](https://github.com/Sprytte/CareerConnect/issues/50), [PR #94](https://github.com/Sprytte/CareerConnect/pull/94) |
| Testing, acceptance tests, rebases and review follow-ups | 3 hours | [Issue #96](https://github.com/Sprytte/CareerConnect/issues/96), [Issue #111](https://github.com/Sprytte/CareerConnect/issues/111), PRs #93, #94 and #100 |
| **Total** | **25 hours** | **Approximate total** |

## AI-assisted resume feedback

For [Issue #59](https://github.com/Sprytte/CareerConnect/issues/59) (task 10.2), I built the first AI feature of CareerConnect: a job seeker can upload a PDF or DOCX resume and receive written feedback.

On the frontend, I added the `/account/resume/feedback` page, linked from the resume page. It accepts a file by picker or drag and drop, checks the type and the 5 MB limit before anything is sent, and shows a loading screen with progress steps and a **Cancel** button while the feedback is generated. The result is shown as a summary, "What works well" and "What to improve", with a note that the text was written by an AI model. Each error the backend can return (wrong file, expired session, unreadable document, service busy, service not set up, timeout) has its own message.

On the backend, I added `POST /api/v1/resumes/feedback` in a new `ResumeFeedbackManagement` package. The service validates the file type, size and PDF signature, extracts the text of DOCX files with the JDK only (external entities disabled, length capped), and sends the resume to Google's Gemini `generateContent` API behind a `ResumeFeedbackGenerator` interface, so the provider can be replaced without touching the controller. The API key is read from the `GEMINI_API_KEY` environment variable and is never committed; without a key the endpoint answers 503 and the page explains that AI feedback is not set up yet. Provider failures are mapped to 429, 502, 503 or 504, and the uploaded file is not stored.

**Evidence:** [Issue #59](https://github.com/Sprytte/CareerConnect/issues/59), [merged PR #100](https://github.com/Sprytte/CareerConnect/pull/100), [acceptance tests #111](https://github.com/Sprytte/CareerConnect/issues/111), handoff notes in `docs/Sprint3-Issue59-Handoff.md`.

## Editing and withdrawing applications

For [Issue #54](https://github.com/Sprytte/CareerConnect/issues/54) (task 14.2), I built the job seeker side of editing and withdrawing an application, using the endpoints from task 14.1.

Each application card on the applications page now has **Edit application** and **Withdraw** actions. Editing lets the user pick another of their resumes or upload a new one, and withdrawing asks for confirmation first. After a change, the list reloads from the server and a confirmation message stays visible. Once the posting's deadline has passed, the actions are removed and the card explains why.

To support this, I extended the account applications response with the attached `resumeId`, the posting `deadline` and a `modifiable` flag, computed in `AccountApplicationService` from whether the posting still exists and its deadline has not passed.

**Evidence:** [Issue #54](https://github.com/Sprytte/CareerConnect/issues/54), [merged PR #93](https://github.com/Sprytte/CareerConnect/pull/93), [acceptance tests #96](https://github.com/Sprytte/CareerConnect/issues/96), handoff notes in `docs/Sprint3-Issue54-Handoff.md`.

## Recruiter view to filter and sort applications

For [Issue #50](https://github.com/Sprytte/CareerConnect/issues/50) (task 11.3), I built the recruiter's **Applications received** page at `/account/jobs/applications`, linked from **My postings** and from a **View applications** button on each posting.

The **Filter** tab narrows postings by status (Open or Closed), applications by status, and either by a date range on date posted, deadline or date applied, with a message when the start date is after the end date. The **Sort** tab orders postings by date posted, deadline, status or number of applications, and applications by date applied or status. The page also handles sign-in and expired sessions, a retry when postings fail to load, a per-posting retry when one posting's applications fail, and empty results. After review, I added a "Fewest applications first" sort.

**Evidence:** [Issue #50](https://github.com/Sprytte/CareerConnect/issues/50), [merged PR #94](https://github.com/Sprytte/CareerConnect/pull/94), handoff notes in `docs/Sprint3-Issue50-Handoff.md`.

## Testing

- Added `ResumeFeedbackServiceTest`, `DocxTextExtractorTest` and `GeminiResumeFeedbackGeneratorTest` for the AI feedback backend. The Gemini tests use a local stand-in server, so they run in CI without an API key or network access.
- Added `AccountApplicationChangeTest` (9 tests) for the new application fields and the deadline rule.
- Added 24 unit tests for the filter and sort rules of the recruiter view in `frontend/tests/postingApplicationsView.test.ts`, and a `frontend-tests` job that runs them in the GitHub Actions workflow.
- Ran the backend Gradle test suite on each branch before pushing.
- Ran the AI feedback acceptance tests (AT-10-1 to AT-10-6) with a real Gemini key, a test PDF and a test DOCX resume that contain no real personal data, and posted the results with screenshots on PR #100 and in [#111](https://github.com/Sprytte/CareerConnect/issues/111).
- Recorded the edit and withdraw acceptance tests in [#96](https://github.com/Sprytte/CareerConnect/issues/96) and the recruiter view checks with screenshots on PR #94.

## Review and collaboration

- Answered review comments on my PRs: added the "Fewest applications first" sort requested on PR #94, and agreed with Emilie to keep the two logic files as `.ts` so the frontend tests could keep running.
- Rebased PR #93 onto `main` to combine my edit and withdraw actions with a teammate's application activity chart in `Applications.tsx`, and rebased PR #100 to keep the new role onboarding (`RoleGate`) alongside the AI feedback route in `main.tsx`.
- Opened [Issue #106](https://github.com/Sprytte/CareerConnect/issues/106) for Sprint 4 to reduce the prompt injection risk raised by Brendan during review of PR #100.
- I did not review teammates' PRs this sprint; instead I took on three implementation issues rather than two.

All three assigned Sprint 3 issues (#50, #54 and #59) were completed and merged through PRs #94, #93 and #100.
