# Sprint 3 Individual Contribution Log

**Name:** Emilie Parent
**GitHub:** [Sprytte](https://github.com/Sprytte)  
**Course:** SOEN 341  
**Project:** [CareerConnect](https://github.com/Sprytte/CareerConnect) 
**Sprint:** 3

## Estimated time spent

The hours below are retrospective estimates of my active work, reconstructed from work sessions and repository activity. They are approximate rather than an exact timesheet.

| Activity | Estimated time | Related work |
| --- | ---: | --- |
| Fix problems on main | 45 minutes | [PR #92](https://github.com/Sprytte/CareerConnect/pull/92) |
| Manage the Projects board, update the backlog | 2 hours | [Project board](https://github.com/users/Sprytte/projects/1/views/6) |
| Meeting minutes | 30 minutes | [Meeting minutes](https://github.com/Sprytte/CareerConnect/blob/main/docs/Meeting%20Minutes/2026-11-11-meeting.md) |
| Add interview time slot option + Send emails on application update and near deadline | 5 hours + 10 hours| [Interview commit](https://github.com/Sprytte/CareerConnect/pull/105/changes/e045856145830feb2b4eefed447724c4f729bb06), [Issue #51](https://github.com/Sprytte/CareerConnect/issues/51), [Issue #52](https://github.com/Sprytte/CareerConnect/issues/52) |
| Code reviews, team coordination, documentation | 2.5 hours | PRs listed below |
| **Total** | **20.5 hours** | **Approximate total** |

## Fix problems on main

I noticed that one of our functionalities, [Allow users to apply](https://github.com/Sprytte/CareerConnect/issues/42), wasn't in the main branch despite the PR having been successfully merged. After some investigation, I noticed it got merged into a different PR after it was already closed. I created a new PR, fixed the merge and conflicts, and merged the changes back into main.

**Evidence** [Fixed PR #92](https://github.com/Sprytte/CareerConnect/pull/92), [Missing PR #78](https://github.com/Sprytte/CareerConnect/pull/78), [Closed PR #77](https://github.com/Sprytte/CareerConnect/pull/77)

## GitHub Projects Board + Backlog

I updated the Projects Board and Backlog to match the updated requirements of Sprint 3 and classified all tasks to have story points and be assigned to a matching Sprint. I added new tasks/issues as they came up and organized them accordingly.


**Evidence:** [Project board](https://github.com/users/Sprytte/projects/1/views/6), [Issues list](https://github.com/Sprytte/CareerConnect/issues), [Sprint 3](https://github.com/Sprytte/CareerConnect/milestone/3).

## Meeting minutes

I organized the meeting minutes, held the meeting, took the notes and uploaded them to the GitHub.

**Evidence:** [Commit](https://github.com/Sprytte/CareerConnect/commit/03b1ccc53bb672f681d7eb98aee86e6185d1a284), [Meeting minutes](https://github.com/Sprytte/CareerConnect/blob/main/docs/Meeting%20Minutes/2026-11-11-meeting.md)

## Add interview time slot + Send emails on application update and near deadline

I merged the two different pages that were implemented to view the applications on a job posting into one page. Given that my following tasks required an interview time for the email reminders to be sent, I then added a new field for the recruiter to add an interview time slot.

I then implemented the emailing service, using the Gmail SMTP, that sends an email whenever someone applies to a recruiter's job posting and whenever an applicant's application gets updated. I also setup a scheduling service that runs every 5 minutes. If the deadline of a job posting has passed, it will get set to 'Archived'. If the deadline of a job posting is within 24 hours, an email will be sent to all its applicants reminding them to finalize their application. If the interview time slot is within 24 hours, a confirmation email will be sent to both the interviewer and the interviewee.

**Evidence:** [PR # 105](https://github.com/Sprytte/CareerConnect/pull/105), [Issue #51](https://github.com/Sprytte/CareerConnect/issues/51), [Issue #52](https://github.com/Sprytte/CareerConnect/issues/52), [Acceptance criteria #51](https://github.com/Sprytte/CareerConnect/issues/114), [Acceptance criteria #52](https://github.com/Sprytte/CareerConnect/issues/112), [Interview commit](https://github.com/Sprytte/CareerConnect/pull/105/changes/e045856145830feb2b4eefed447724c4f729bb06)

## Code reviews

I have done code reviews on the following PRs:
- [PR #97](https://github.com/Sprytte/CareerConnect/pull/97)
- [PR #94](https://github.com/Sprytte/CareerConnect/pull/94)
- [PR #98](https://github.com/Sprytte/CareerConnect/pull/98)
- [PR #93](https://github.com/Sprytte/CareerConnect/pull/93)
- [PR #95](https://github.com/Sprytte/CareerConnect/pull/95)
- [PR #101](https://github.com/Sprytte/CareerConnect/pull/101)
