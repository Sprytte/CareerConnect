# Sprint 3: request resume feedback from AI (issue #59, task 10.2)

Built on main `90b1c03`.

## Interface

New page at `/account/resume/feedback`, linked from the resume page
("Get AI feedback on a resume").

1. **Accepts a resume file**: file picker or drag and drop, PDF or DOCX up to
   5 MB. Type, empty-file and size problems are reported before anything is
   sent.
2. **Loading screen** while the AI generates a response: the form is replaced
   by a progress screen with the file name, a status line that advances, and
   **Cancel**. The request is abandoned after 90 seconds.
3. **Displays the feedback**: a short summary, "What works well" and "What to
   improve", plus a note that the text is AI-generated. The feedback is
   rendered as plain text.

The page tells the user the resume is sent to an external AI service and that
the file chosen there is not saved to their account.

## Backend

`POST /api/v1/resumes/feedback` (multipart `file`), documented in
`docs/API.md`. Code is in `ResumeFeedbackManagement`:

- `ResumeFeedbackService` validates the upload and prepares it. PDFs go to the
  model as documents; DOCX text is extracted with the JDK only.
- `ResumeFeedbackGenerator` is the provider interface;
  `GeminiResumeFeedbackGenerator` implements it with the Gemini
  `generateContent` REST API. No new dependencies.
- Nothing is stored, and the key, the resume and the reply are never logged.

### Configuration

Set the key in the environment before starting the backend. Do not put it in
`application.properties`.

```powershell
$env:GEMINI_API_KEY = "your-key"
# Optional: $env:GEMINI_MODEL = "another-model-id"
```

Without a key the endpoint answers 503 and the page says AI feedback is not
set up yet. Free-tier Gemini keys may allow Google to use submitted content to
improve its products, so use test resumes while on a free key.

## Error handling

| Backend answer | What the user sees |
| --- | --- |
| 400, 413, 415 | The file was not accepted |
| 401 | Session expired, with a sign-in link |
| 422 | No readable text (for example a scanned document) |
| 429 | The AI service is busy; try again in a minute |
| 503 | AI feedback is not set up on this server |
| 404, 405 (backend without the endpoint) | Not available yet |
| 502, 504, anything else, or an unreadable 200 | Could not produce feedback; try again |
| No answer within 90 s | The AI service took too long |

Every failure returns to the form with the message, so the user can retry.

## Replacing the provider

Implement `ResumeFeedbackGenerator` in another class and remove `@Component`
from the Gemini one. The endpoint, the page and the JSON contract stay the same.

## Tests

Backend unit tests are in `DocxTextExtractorTest`, `ResumeFeedbackServiceTest`
and `GeminiResumeFeedbackGeneratorTest` and run with the Gradle build in CI.
They use a local stand-in for the AI provider, so they need no key and make
no outside requests.

Acceptance tests for the user story are in
`docs/Acceptance Tests/US-10-AI-assisted-resume-feedback.md`.
