# CareerContact API

HTTP endpoints currently implemented by the CareerContact Spring Boot backend.

## Local configuration

```text
Backend:  http://localhost:8080
Frontend: http://localhost:3000
```

These values are configured in
`CareerContactAPI/src/main/resources/application.properties` and
`CareerContactAPI/src/main/resources/application.yml`.

## Authentication flow

Authentication uses Auth0 through Spring Security OAuth2/OIDC:

1. Open `GET /oauth2/authorization/okta` in a browser.
2. Complete authentication with Auth0.
3. Spring Security handles `GET /login/oauth2/code/okta`.
4. The backend processes `GET /api/v1/cc/security/redirect`.
5. The backend creates the local user if necessary, sets cookies, and redirects
   to the frontend root or `/admin`.

The login flow is browser-based rather than a JSON API. The post-login response
sets these cookies:

| Cookie | Value |
| --- | --- |
| `id_token` | OIDC ID token |
| `isAuthenticated` | `true` |
| `accessPermission` | User role/authority information |
| `picture` | Profile picture URL |
| `userId` | Auth0 subject identifier |

## Authentication endpoints

### Start login

**Request**

```http
GET /oauth2/authorization/okta
```

**Purpose**

Starts the Auth0 login flow. Open this URL in a browser.

**Returns**

An HTTP redirect to Auth0. This endpoint does not return application JSON.

### OAuth callback

**Request**

```http
GET /login/oauth2/code/okta
```

**Purpose**

Receives the Auth0 callback and is processed internally by Spring Security.

**Returns**

The application continues to
`GET /api/v1/cc/security/redirect`. The frontend should not call this route
directly. It does not return application JSON.

### Complete login

**Request**

```http
GET /api/v1/cc/security/redirect
```

**Purpose**

Completes application-specific login processing and creates a local user if
the authenticated Auth0 user does not already exist.

**Returns**

An HTTP redirect to `http://localhost:3000/` or
`http://localhost:3000/admin`. The response also sets the authentication
cookies listed above. It does not return application JSON.

If there is no authenticated principal, the request redirects to
`/oauth2/authorization/okta`.

### Logout

**Request**

```http
POST /api/v1/careercontact/logout
```

For a browser session, include credentials:

```js
await fetch("http://localhost:8080/api/v1/careercontact/logout", {
  method: "POST",
  credentials: "include"
});
```

**Purpose**

Clears authentication cookies and initiates Auth0 logout.

**Body**

None.

**Returns**

`200 OK` with an empty response body. This endpoint does not return JSON.

### Get the current session user

**Request**

```http
GET /api/v1/cc/security/me
```

For a browser session, include credentials:

```js
const response = await fetch("http://localhost:8080/api/v1/cc/security/me", {
  credentials: "include"
});
```

**Purpose**

Returns the user associated with the currently authenticated server session.
The endpoint does not require a `userId` path parameter. After profile integration
(PR #64), name, contact email, and picture come from the persisted database
profile so old login claims do not overwrite saved changes.

**Body**

None.

**Returns**

`200 OK` with a `UserInfoResponseModel` JSON object:

| Field | Type | Description |
| --- | --- | --- |
| `userId` | string | Auth0 user identifier. |
| `email` | string | User email address. |
| `name` | string | User display name. |
| `picture` | string | Profile picture URL. |
| `user_metadata` | object | Not populated by this endpoint. |

`401 Unauthorized` if there is no authenticated server session.

### Get a persisted user profile

**Request**

```http
GET /api/v1/users/{userId}
```

**Purpose**

Returns the profile stored in the local CareerContact database for `userId`.
The authenticated subject must match `userId`: no session returns 401 and a
different account returns 403. `/me` reads the same persisted profile values.

**Body**

None.

**Returns**

`200 OK` with a `UserResponseModel` JSON object:

| Field | Type | Description |
| --- | --- | --- |
| `userId` | string | Local user identifier, matching the Auth0 subject. |
| `name` | string | Saved display name. |
| `email` | string | Saved email address. |
| `pictureUrl` | string | Saved profile picture URL, if present. |

`404 Not Found` if no local user exists with the supplied `userId`.

### Update a persisted user profile

**Request**

```http
PUT /api/v1/users/{userId}
Content-Type: application/json
```

For a browser session, include credentials:

```js
const currentUser = await fetch("http://localhost:8080/api/v1/cc/security/me", {
  credentials: "include"
}).then(async response => {
  if (!response.ok) {
    throw new Error(`Failed to fetch current user: ${response.status}`);
  }
  return response.json();
});

const updatedUser = await fetch(
  `http://localhost:8080/api/v1/users/${encodeURIComponent(currentUser.userId)}`,
  {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      name: "Updated Name",
      email: "updated@example.com"
    })
  }
).then(async response => {
  if (!response.ok) {
    throw new Error(`Failed to update user: ${response.status} ${await response.text()}`);
  }
  return response.json();
});
```

**Purpose**

Updates the locally persisted name and contact email for the authenticated
account. This does not change the provider-managed sign-in email. No session
returns 401 and a different account returns 403.

**Body**

A `UserUpdateRequestModel` JSON object:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `name` | string | Yes | New display name. Must not be blank. |
| `email` | string | Yes | New email address. Must not be blank. |
| `password` | string | No | Rejected here. Use the separate password endpoint below. |

**Returns**

`200 OK` with the updated `UserResponseModel` JSON object described above.

`400 Bad Request` if name/email is invalid or a password field is supplied.
`409 Conflict` if the contact email is already in use.

`404 Not Found` if no local user exists with the supplied `userId`.

### Change the authenticated account password

```http
PUT /api/v1/users/{userId}/password
Content-Type: application/json

{"password":"a-new-password-meeting-the-provider-policy"}
```

Requires the matching signed-in OIDC subject. This endpoint supports primary
Auth0 database identities (`auth0|...`); external-provider identities return
409 and must use that provider's password settings. The server enforces a
15-character minimum, uses the existing Auth0 Management API credentials,
checks the upstream response, and returns 204 only after confirmation. The
current local session is invalidated on success. The password is never stored
in the local database or returned in the response.

Errors: 400 for length/provider-policy rejection, 401 without a session, 403
for a different account, 409 for external-provider accounts, and 502 for an
unavailable/misconfigured upstream service or invalid confirmation. Upstream
response bodies and credentials are not exposed. Live tenant permissions and
password policy still need to be checked with a disposable test account.

## User and account endpoints

### Get user information

**Request**

```http
GET /api/v1/cc/security/user-info/{userId}
```

**Purpose**

Retrieves the Auth0 profile associated with `userId`.

**Body**

None.

**Returns**

`200 OK` with a `UserInfoResponseModel` JSON object:

| Field | Type | Description |
| --- | --- | --- |
| `userId` | string | Auth0 user identifier. |
| `email` | string | User email address. |
| `name` | string | User display name. |
| `picture` | string | Profile picture URL. |
| `user_metadata` | object | Auth0 metadata as string key/value pairs. |

### Update user information

**Request**

```http
PATCH /api/v1/cc/security/user-info/{userId}
Content-Type: application/json
```

**Purpose**

The current implementation forwards only **name** to Auth0 for the specified
`userId`. It does not forward email or implement password changes.

Local Google sign-in testing on October 8 confirmed that Auth0 rejects name
updates with HTTP 400 and `operation_not_supported` under the current connection
sync policy. The local API maps that error into empty profile fields and still
returns 201. Successful name persistence remains unverified until this is fixed.

**Body**

A `UserRequestModel` JSON object:

| Field | Type | Description |
| --- | --- | --- |
| `userId` | string | User identifier. |
| `name` | string | Updated display name. |
| `email` | string | Accepted by the request model but ignored by the current update service. |
| `pictureUrl` | string | Accepted by the request model but ignored by the current update service. |

**Returns**

`201 Created` with the updated `UserInfoResponseModel` JSON object described
above. The current service can also return null or incomplete profile data
after an Auth0 failure while the controller still returns 201. Consumers must
not treat the status code alone as confirmation of a saved update. The account
interface now uses the persisted `/api/v1/users` routes and separate password
endpoint instead. Legacy user-info reads, writes, and account deletion require
matching ownership; employee administration requires the verified Admin claim.

### Delete a user account

**Request**

```http
DELETE /api/v1/cc/security/deleteAccount/users/{userId}
```

**Purpose**

Deletes the Auth0 user account associated with `userId`.

**Body**

None.

**Returns**

`200 OK` with an empty response body, or `500 Internal Server Error` if the
Auth0 deletion fails. This endpoint does not return JSON.

## Job posting endpoints

### Apply to a job posting

**Request**

```http
POST /api/v1/job-postings/{jobId}/apply
Content-Type: application/json
```

For a browser session, include credentials:

```js
const response = await fetch(
  "http://localhost:8080/api/v1/job-postings/1/apply",
  {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "Accept": "application/json"
    },
    body: JSON.stringify({})
  }
);
```

**Purpose**

Creates a job application for the currently authenticated user. The
authenticated user's Auth0 subject is used as the applicant; it is not
provided in the request body.

**Body**

The body is optional. To apply with a specific resume, provide its ID:

```json
{
  "resumeId": 1
}
```

If `resumeId` is omitted, the user's most recently uploaded resume is used.
The user must have at least one resume, and a user can only apply with a
resume that belongs to them.

**Returns**

`201 Created` with a `JobApplicationResponseModel` JSON object:

| Field | Type | Description |
| --- | --- | --- |
| `id` | integer | Created application ID. |
| `jobPostingId` | integer | ID of the job posting. |
| `userId` | string | Auth0 subject of the applicant. |
| `resumeId` | integer | Resume submitted with the application. |
| `status` | string | Initial application status, `Applied` (older records may contain `SUBMITTED`). |
| `appliedAt` | string | Application timestamp in ISO-8601 format. |

Possible errors:

| Status | Description |
| --- | --- |
| `400 Bad Request` | The user has no resume and no resume can be selected. |
| `400 Bad Request` | The deadline for the job posting has passed. |
| `401 Unauthorized` | No authenticated browser session was supplied. |
| `403 Forbidden` | The selected resume belongs to another user. |
| `404 Not Found` | The job posting or selected resume does not exist. |
| `409 Conflict` | The user has already applied to the job posting. |

## Job-seeker application list

```http
GET /api/v1/account/applications
```

Requires an authenticated OIDC session. The server derives the account from
that session and returns all of its applications ordered by applied time and
ID descending. There is no caller-selected user ID. A second account's records
are excluded at the repository query. No session returns 401. The response is
not cached (`Cache-Control: no-store`). An empty account returns `[]`.

Each object has integer `id`, string `jobTitle`, string `company`, nullable
string `status`, and nullable date-only `appliedOn` (`YYYY-MM-DD`). Posting details
are fetched in one batch. A deleted posting retains its application record with
neutral missing-posting labels. Legacy `SUBMITTED`, `Offer`, and `Reject` map to
`Applied`, `Offered`, and `Rejected`. Unknown statuses stay unknown and display
neutrally in the interface. Status writes remain a separate recruiter workflow.

### Edit an application

```http
PUT /api/v1/account/applications/{applicationId}
Content-Type: application/json
```

The authenticated applicant can change the resume attached to the application
before the job posting deadline:

```json
{
  "resumeId": 2
}
```

The resume must belong to the authenticated user. The endpoint returns the
updated application summary. It returns `401 Unauthorized` without a session,
`403 Forbidden` for another user's application or resume, `404 Not Found` when
the application, posting, or resume does not exist, and `400 Bad Request` when
the resume is missing or the posting deadline has passed.

### Withdraw an application

```http
DELETE /api/v1/account/applications/{applicationId}
```

Only the applicant can withdraw the application, and withdrawal is allowed
before the job posting deadline. The endpoint returns `204 No Content`.

## Application endpoints

### Update an applicant's status

**Request**

```http
PUT /api/v1/account/job-postings/{jobId}/applications
Content-Type: application/json
```

The authenticated user must own the job posting. Identify the application in
the request body and provide one of the allowed statuses:

```json
{
  "applicationId": 1,
  "status": "Interview"
}
```

`Offer` and `Reject` are also valid statuses. The endpoint returns the updated
application. It returns `401 Unauthorized` without an authenticated user,
`403 Forbidden` when the user does not own the job posting, `404 Not Found`
when the job posting or application does not exist, and `400 Bad Request` for
a missing or invalid status/application ID.

## Resume endpoints

All resume endpoints require an authenticated browser session. Include
`credentials: "include"` when calling them from the frontend. The server
derives the owner from the authenticated user's OIDC subject; clients do not
send a `userId` path parameter. Resume download, update, and delete requests
also verify that the requested resume belongs to the authenticated user.

### Upload a resume

**Request**

```http
POST /api/v1/resumes
Content-Type: multipart/form-data
```

The multipart request must contain a `file` part. PDF and DOCX files up to
5 MB are supported.

**Returns**

`201 Created` with a `ResumeResponseModel` JSON object, or `401 Unauthorized`
without an authenticated session.

### List the current user's resumes

**Request**

```http
GET /api/v1/resumes
```

**Returns**

`200 OK` with an array of `ResumeResponseModel` objects, or `401 Unauthorized`
without an authenticated session. The list is scoped to the current user.

### Update a resume filename

**Request**

```http
PUT /api/v1/resumes/{resumeId}
Content-Type: text/plain
```

The request body is the new filename, including its extension. The file
contents are not changed.

**Returns**

`200 OK` with the updated `ResumeResponseModel`, `400 Bad Request` for a blank
filename, `401 Unauthorized` without an authenticated session, `403 Forbidden`
when the resume belongs to another user, or `404 Not Found` when the resume
does not exist.

### Download a resume

**Request**

```http
GET /api/v1/resumes/{resumeId}/download
```

**Returns**

`200 OK` with the resume file, `401 Unauthorized` without an authenticated
session, `403 Forbidden` when the resume belongs to another user, or
`404 Not Found` when the resume or stored file does not exist.

### Delete a resume

**Request**

```http
DELETE /api/v1/resumes/{resumeId}
```

**Returns**

`204 No Content` when the resume is deleted, `401 Unauthorized` without an
authenticated session, `403 Forbidden` when the resume belongs to another
user, or `404 Not Found` when the resume does not exist.

## Current security behavior

The routes and authentication infrastructure are configured in:

```text
CareerContactAPI/src/main/java/com/example/careercontactapi/security/SecurityConfig.java
CareerContactAPI/src/main/java/com/example/careercontactapi/security/SecurityController.java
CareerContactAPI/src/main/java/com/example/careercontactapi/security/service/Auth0LoginService.java
```

`SecurityConfig` uses `.anyRequest().permitAll()` for routes without a more
specific rule. The `GET /api/v1/cc/security/me` endpoint and all
`/api/v1/resumes/**` endpoints explicitly require an authenticated session.
Resume controller and service checks scope list/upload operations to the
authenticated subject and ensure users can only download or delete their own
resumes. Other custom API endpoints may still be reachable without an
authenticated session unless another configuration or deployment layer
restricts them. The job-application and job-seeker application-list endpoints
also enforce authentication and derive the applicant account from the current
session rather than accepting a caller-supplied user ID.

Public job-posting reads are intentional. JWT validation alone does not require
authentication on every route, so each endpoint's authorization rules should be
read independently rather than treating successful JWT configuration as proof
that all routes are protected.

If Spring Security rejects a request as unauthenticated, its configured JSON
error response has this field:

| Field | Type | Description |
| --- | --- | --- |
| `message` | string | Authentication error message. |

The endpoint behavior documented here is based on the current source code.
