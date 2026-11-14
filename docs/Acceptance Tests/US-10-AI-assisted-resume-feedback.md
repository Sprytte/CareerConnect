# Acceptance tests: US-10 AI-assisted resume feedback

**User story:** As a job seeker, I want to receive AI-assisted feedback on my
resume so that I can identify areas that could be improved before applying
for jobs.

**Covers:** issue #59 (task 10.2).

**Setup for all tests:** backend started with `GEMINI_API_KEY` set, frontend
running locally. A text-based PDF resume and a DOCX resume that contain no
real personal data. Sign in as a job seeker, open **Resumes** and choose
**Get AI feedback on a resume**.

## AT-10-1 Get feedback on a PDF resume

- **Given** I am on the AI feedback page
- **When** I select the PDF resume and choose **Get AI feedback**
- **Then** a loading screen shows the file name and a progress message
- **And** when it finishes I see a summary, "What works well" and "What to improve" for that file
- **And** the page says the feedback was written by an AI model

## AT-10-2 Get feedback on a DOCX resume

- **Given** I am on the AI feedback page
- **When** I select the DOCX resume and choose **Get AI feedback**
- **Then** I see feedback that refers to the content of that resume

## AT-10-3 Unsupported files are refused before anything is sent

- **Given** I am on the AI feedback page
- **When** I select a `.txt` file
- **Then** I am told to choose a PDF or DOCX file
- **When** I choose **Get AI feedback** without selecting a file
- **Then** I am told to choose a resume file first and no loading screen appears

## AT-10-4 Cancel while the feedback is being generated

- **Given** I submitted a resume and the loading screen is showing
- **When** I choose **Cancel**
- **Then** I am back on the form with no error and can submit again

## AT-10-5 The service not being set up is explained

- **Given** the backend was started without `GEMINI_API_KEY`
- **When** I submit a PDF resume
- **Then** I return to the form with a message that AI feedback is not set up on this server yet

## AT-10-6 Only signed-in users can request feedback

- **Given** I am signed out
- **When** I open `/account/resume/feedback`
- **Then** I am asked to sign in and no upload form is shown

## Results

| Test | Result | Date | Tested by | Notes |
| --- | --- | --- | --- | --- |
| AT-10-1 | Not run | | | |
| AT-10-2 | Not run | | | |
| AT-10-3 | Not run | | | |
| AT-10-4 | Not run | | | |
| AT-10-5 | Not run | | | |
| AT-10-6 | Not run | | | |
