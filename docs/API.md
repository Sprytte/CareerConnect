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

Updates the Auth0 profile associated with `userId`.

**Body**

A `UserRequestModel` JSON object:

| Field | Type | Description |
| --- | --- | --- |
| `userId` | string | User identifier. |
| `name` | string | Updated display name. |
| `email` | string | User email address. |
| `pictureUrl` | string | Profile picture URL. |

**Returns**

`201 Created` with the updated `UserInfoResponseModel` JSON object described
above.

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

## Resume endpoints

All resume endpoints require an authenticated browser session. Include
`credentials: "include"` when calling them from the frontend. The `userId`
path parameter must match the authenticated user's Auth0 subject identifier.
Resume download and delete requests also verify that the requested resume
belongs to the authenticated user.

### Upload a resume

**Request**

```http
POST /api/v1/resumes/users/{userId}
Content-Type: multipart/form-data
```

The multipart request must contain a `file` part. PDF and DOCX files up to
5 MB are supported.

**Returns**

`201 Created` with a `ResumeResponseModel` JSON object, `401 Unauthorized`
without an authenticated session, or `403 Forbidden` when `{userId}` does not
match the authenticated user.

### List the current user's resumes

**Request**

```http
GET /api/v1/resumes/users/{userId}
```

**Returns**

`200 OK` with an array of `ResumeResponseModel` objects, `401 Unauthorized`
without an authenticated session, or `403 Forbidden` when `{userId}` does not
match the authenticated user.

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
Resume controller and service checks additionally enforce that users can only
list or upload against their own user ID and can only download or delete their
own resumes. Other custom API endpoints may still be reachable without an
authenticated session unless another configuration or deployment layer
restricts them.

If Spring Security rejects a request as unauthenticated, its configured JSON
error response has this field:

| Field | Type | Description |
| --- | --- | --- |
| `message` | string | Authentication error message. |

The endpoint behavior documented here is based on the current source code.
