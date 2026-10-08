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
The endpoint does not require a `userId` path parameter.

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
Unlike `GET /api/v1/cc/security/me`, this endpoint returns the locally saved
profile values.

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
      email: "updated@example.com",
      password: "new-password"
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

Updates the locally persisted name and email for `userId`.

**Body**

A `UserUpdateRequestModel` JSON object:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `name` | string | Yes | New display name. Must not be blank. |
| `email` | string | Yes | New email address. Must not be blank. |
| `password` | string | No | Accepted for compatibility with the profile request. It is not stored locally; authentication credentials remain managed by Auth0. |

**Returns**

`200 OK` with the updated `UserResponseModel` JSON object described above.

`400 Bad Request` if `name` or `email` is missing or blank.

`404 Not Found` if no local user exists with the supplied `userId`.

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
not treat the status code alone as confirmation of a saved update. No password
field or user-password update endpoint is implemented in this snapshot.

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
| `status` | string | Initial application status, currently `SUBMITTED`. |
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

## Current security behavior

The routes and authentication infrastructure are configured in:

```text
CareerContactAPI/src/main/java/com/example/careercontactapi/security/SecurityConfig.java
CareerContactAPI/src/main/java/com/example/careercontactapi/security/SecurityController.java
CareerContactAPI/src/main/java/com/example/careercontactapi/security/service/Auth0LoginService.java
```

At present, `SecurityConfig` uses `.anyRequest().permitAll()`, and several
controller-level authentication checks are commented out. Consequently, most
custom API endpoints are currently reachable without an authenticated session
unless another configuration or deployment layer restricts them. The
`GET /api/v1/cc/security/me` endpoint is an exception and explicitly requires
an authenticated session. JWT validation is configured, but it is not the same
as requiring authentication on every route.

If Spring Security rejects a request as unauthenticated, its configured JSON
error response has this field:

| Field | Type | Description |
| --- | --- | --- |
| `message` | string | Authentication error message. |

The endpoint behavior documented here is based on the current source code and
may change when route authorization is enabled.
