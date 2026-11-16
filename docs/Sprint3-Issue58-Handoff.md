# Sprint 3: integrate AI for resume analysis (issue #58, task 10.1)

## What this adds

The AI provider integration from task 10.2 (PR #100) is reused unchanged:
`GeminiResumeFeedbackGenerator` still owns the Gemini `generateContent` call,
key handling, timeout and provider error mapping. Task 10.1 adds the endpoint
that reviews a resume already saved to the signed-in user's account.

## Endpoint

```http
POST /api/v1/resumes/{resumeId}/feedback
```

`POST /api/v1/account/resumes/{resumeId}/feedback` is an alias for the
account-scoped path named in the issue. Both routes:

- require an authenticated `JOB_SEEKER` session;
- take the owner from the OIDC subject, not from the request body;
- resolve the resume and its stored file through `ResumeService`, which
  returns `403` for another user's resume and `404` for a missing resume/file;
- return the same `ResumeFeedbackResponseModel` as the file-upload route, with
  `Cache-Control: no-store`.

The existing `POST /api/v1/resumes/feedback` upload route remains available
for the task 10.2 page. The new route does not create, change or delete a
resume.

## Code

- `ResumeFeedbackManagement/businesslayer/ResumeFeedbackService` adds
  `requestFeedbackForResume(userId, resumeId)`. It shares the upload route's
  validation and DOCX extraction, and reads stored files through a bounded
  stream so a large file cannot be loaded accidentally.
- `ResumeFeedbackManagement/presentationlayer/SavedResumeFeedbackController`
  maps the endpoint, checks the job-seeker role and disables caching.
- Ownership stays in `ResumeServiceImpl.findOwnedResume`, so the new route
  follows the same rule as download/update/delete.

## Error handling

| Status | Meaning |
| --- | --- |
| `400` | Invalid resume ID, or the stored file is empty, oversized, unsupported or invalid. |
| `401` | No authenticated session. |
| `403` | The resume belongs to another user, or the account is not a job seeker. |
| `404` | The resume row or its stored file does not exist. |
| `422` | A stored DOCX has no readable text. |
| `429` | Gemini is rate limiting requests. |
| `502` | Gemini failed or returned no usable feedback. |
| `503` | `GEMINI_API_KEY` is missing or rejected. |
| `504` | Gemini did not answer in time. |

Configuration is unchanged: set `GEMINI_API_KEY` in the environment, and
optionally `GEMINI_MODEL`; never commit the key.

## Tests

`SavedResumeFeedbackServiceTest` covers PDF and DOCX files read from the
owner's saved resume, path trimming, authentication, invalid IDs, ownership
errors from `ResumeService`, invalid/empty/oversized files and the unreadable
DOCX case. `SavedResumeFeedbackControllerTest` covers the signed-out, signed-in
job-seeker, recruiter-rejection, missing-resume and account-alias paths.

Run them with:

```bash
cd CareerContactAPI
bash gradlew test --tests 'com.example.careercontactapi.ResumeFeedbackManagement.*'
```

The full backend suite still runs in CI. No real Gemini key is needed for the
unit tests because they use a local stand-in or a fake generator.

## Not changed

The task 10.2 page still uploads a file to `POST /api/v1/resumes/feedback`, so
its existing acceptance tests continue to apply. A follow-up UI task can offer
the saved-resume route from the resume list if the team wants that flow.
