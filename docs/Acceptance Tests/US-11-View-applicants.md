# Acceptance tests: US-11 View applicants

**User story:** As a recruiter, I want to be able to view the applications on
my job posting so that I can then have the option to reject, setup interview,
or make an offer to each applicant.

**Covers:** issue #50 (task 11.3), viewing and filtering applications.

**Setup for all tests:** backend and frontend running locally. A recruiter
account with two job postings: posting A with a deadline in the future and at
least two applications in different statuses, and posting B whose deadline
has passed with at least one application. Sign in as the recruiter.

## AT-11-1 View the applications received on my postings

- **Given** I am on **My postings**
- **When** I choose **Applications received**
- **Then** I see both of my postings, each marked Open or Closed
- **And** each posting lists its applications with the date applied and the status
- **And** the summary line shows how many postings and applications are shown

## AT-11-2 Filter by status

- **Given** I am on **Applications received** with the **Filter** tab open
- **When** I set **Posting status** to Closed
- **Then** only posting B is shown
- **When** I choose **Clear filters** and set **Application status** to Interview
- **Then** only postings with an application in Interview are shown, listing only those applications

## AT-11-3 Filter by date

- **Given** I am on **Applications received** with the **Filter** tab open
- **When** I set **Date to filter** to Date applied and enter a **From** and **To** range that contains only one application
- **Then** only that application and its posting are shown
- **When** I enter a **From** date that is after the **To** date
- **Then** a message says the start date must be on or before the end date and the list is not narrowed

## AT-11-4 Sort by status or date

- **Given** I am on **Applications received**
- **When** I open the **Sort** tab and choose "Status: closed postings first"
- **Then** posting B is listed before posting A
- **When** I choose "Date posted: oldest first"
- **Then** the posting created first is listed first
- **When** I set **Sort applications by** to "Date applied: oldest first"
- **Then** each posting lists its earliest application first

## AT-11-5 Open the applications of one posting

- **Given** I am on **My postings**
- **When** I choose **View applications** on posting A
- **Then** only posting A and its applications are shown
- **And** **Show all my postings** brings back the full list

## AT-11-6 Only the owner sees the applications

- **Given** I am signed out
- **When** I open `/account/jobs/applications`
- **Then** I am asked to sign in and no postings or applications are shown

## Results

| Test | Result | Date | Tested by | Notes |
| --- | --- | --- | --- | --- |
| AT-11-1 | Not run | | | |
| AT-11-2 | Not run | | | |
| AT-11-3 | Not run | | | |
| AT-11-4 | Not run | | | |
| AT-11-5 | Not run | | | |
| AT-11-6 | Not run | | | |
