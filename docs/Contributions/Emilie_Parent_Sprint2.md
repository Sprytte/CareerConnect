# Sprint 2 Individual Contribution Log

**Name:** Emilie Parent
**GitHub:** [Sprytte](https://github.com/Sprytte)  
**Course:** SOEN 341  
**Project:** [CareerConnect](https://github.com/Sprytte/CareerConnect)  
**Sprint:** 2

## Estimated time spent

The hours below are retrospective estimates of my active work, reconstructed from work sessions and repository activity. They are approximate rather than an exact timesheet.

| Activity | Estimated time | Related work |
| --- | ---: | --- |
| Manage the Projects board, update the backlog | 2 hours | [Project board](https://github.com/users/Sprytte/projects/1/views/6)
| Keep the DB data stored in the repository and setup GitHub workflows | 3 hours | [Issue #60](https://github.com/Sprytte/CareerConnect/issues/60), [PR #61](https://github.com/Sprytte/CareerConnect/pull/61) |
| API to create and get job postings | 1 hour | [Issue #33](https://github.com/Sprytte/CareerConnect/issues/33), [PR #63](https://github.com/Sprytte/CareerConnect/pull/63) |
| Interface for users to manage their resumes | 4 hours | [PR #30](https://github.com/Sprytte/CareerConnect/issues/30), [PR #69](https://github.com/Sprytte/CareerConnect/pull/69) |
| Code review, team coordination, and documentation | 6 hours | handoff documents linked below |
| **Total** | **16 hours** | **Approximate total** |

## GitHub Projects Board + Backlog

I created the Projects Board and modified it to better suit our needs. I organized the Sprint View, classified all tasks to have story points and be assigned to a matching Sprint. I added new tasks/issues as they came up and organized them accordingly.


**Evidence:** [Project board](https://github.com/users/Sprytte/projects/1/views/6), [Issues list](https://github.com/Sprytte/CareerConnect/issues), [Sprint 2](https://github.com/Sprytte/CareerConnect/milestone/2).

## Implement a local persistent DB + GitHub Workflows

I first modified the kept the yaml so that the Sprintboot project would create a .sqlite3 file in the repo, thereby allowing us to commit any changes to our stored data and test the project more effectively.

I then created te gradle.yml file in the github workflows root folder of our project and did a lot of trial and error configuring the gradle dependencies to match the ones that GitHub would be running when generating a build. It now generates a build on every PR, whenever a new commit is pushed to the PR, and when changes occur on main.

**Evidence:** [Issue #60](https://github.com/Sprytte/CareerConnect/issues/60), [PR #61](https://github.com/Sprytte/CareerConnect/pull/61).

## Create and Get job postings API

I created the new JobPostingManagement submodule and added the files in the data, business, and presentation layers. I designed the JobPosting object, database table, and request/response models. I then implemented the API and matching logic for users to create a job posting, get all their job postings, and get one specific job posting.

During the PR, I was given some feedback that I then implemented.

**Evidence:** [Issue #33](https://github.com/Sprytte/CareerConnect/issues/33), [PR #63](https://github.com/Sprytte/CareerConnect/pull/63).

## Code review, team coordination, and documentation

I setup the meeting and took the meeting minutes. I clarified the tasks for Sprint 2, explained the issues and made sure everyone was comfortable with their assigned tasks. I set a branch rule to protect the main branch and prevent merging PRs without an approval, updated the README.md and the API.md to reflect the appropriate changes.

I have done code reviews on the following PRs:
- [PR #81](https://github.com/Sprytte/CareerConnect/pull/81)
- [PR #78](https://github.com/Sprytte/CareerConnect/pull/78)
- [PR #77](https://github.com/Sprytte/CareerConnect/pull/77)
- [PR #76](https://github.com/Sprytte/CareerConnect/pull/76)
- [PR #75](https://github.com/Sprytte/CareerConnect/pull/75)
- [PR #74](https://github.com/Sprytte/CareerConnect/pull/74)
- [PR #73](https://github.com/Sprytte/CareerConnect/pull/73)
- [PR #72](https://github.com/Sprytte/CareerConnect/pull/72)
- [PR #71](https://github.com/Sprytte/CareerConnect/pull/71)
- [PR #70](https://github.com/Sprytte/CareerConnect/pull/70)
- [PR #60](https://github.com/Sprytte/CareerConnect/pull/68)
- [PR #67](https://github.com/Sprytte/CareerConnect/pull/67)
- [PR #66](https://github.com/Sprytte/CareerConnect/pull/66)
- [PR #65](https://github.com/Sprytte/CareerConnect/pull/65)
- [PR #64](https://github.com/Sprytte/CareerConnect/pull/64)

**Evidence:** [Meeting Minute](https://github.com/Sprytte/CareerConnect/commit/fa5f10237f42e1561239487435609d0d49490924), [README](https://github.com/Sprytte/CareerConnect/commit/b2a380b5991a8cffd2405f93de2ea58c33dd8613), [API](https://github.com/Sprytte/CareerConnect/commit/8110aa7e541c847ea2638b2f7a6ff243bc96e40b)
