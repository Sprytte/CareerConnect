# Acceptance tests: US-14 Edit or withdraw application

**User story:** As a job seeker, I want to be able to edit or withdraw any of
my applications (that haven't passed the deadline) so that I have the option
to change my mind.

**Covers:** issue #54 (task 14.2) with the endpoints from task 14.1.

**Setup for all tests:** backend and frontend running locally. A job seeker
account with two uploaded resumes. One job posting whose deadline is in the
future and one whose deadline has passed, each with an application from this
account. Sign in as the job seeker and open **Applications**.

## AT-14-1 Edit an application before the deadline

- **Given** my application to the open posting uses my first resume
- **When** I choose **Edit application**, select my second resume and choose **Save changes**
- **Then** a confirmation says the application now uses the second resume
- **And** opening **Edit application** again shows the second resume marked "(current)"

## AT-14-2 Withdraw an application before the deadline

- **Given** my application to the open posting is listed
- **When** I choose **Withdraw**, then **Withdraw application**
- **Then** a confirmation says the application was withdrawn
- **And** the application is no longer in the list and the count is one lower
- **And** it is still gone after refreshing the page

## AT-14-3 Changes are not offered after the deadline

- **Given** my application to the posting whose deadline has passed is listed
- **When** I look at that application
- **Then** there is no **Edit application** or **Withdraw** button
- **And** the card says the deadline has passed and the application can no longer be edited or withdrawn

## AT-14-4 Cancelling keeps the application unchanged

- **Given** my application to the open posting is listed
- **When** I choose **Withdraw**, then **Keep application**
- **Then** the application is still listed with the same resume and status

## AT-14-5 A wrong file is refused when editing

- **Given** I chose **Edit application** and **Upload a new resume**
- **When** I select a file that is not a PDF or DOCX
- **Then** I am told to choose a PDF or DOCX file and **Save changes** stays disabled

## Results

| Test | Result | Date | Tested by | Notes |
| --- | --- | --- | --- | --- |
| AT-14-1 | Not run | | | |
| AT-14-2 | Not run | | | |
| AT-14-3 | Not run | | | |
| AT-14-4 | Not run | | | |
| AT-14-5 | Not run | | | |
