# Supabase Authentication (register / login / forgot / reset)

This module delegates authentication to Supabase's built-in Auth service (**GoTrue**).
There is **no local JWT and no local password hashing in this flow** — our backend is a
thin reactive proxy to `{SUPABASE_URL}/auth/v1/...`, plus a **mirror** of each user into
our own `khotel_user` table so the rest of the app (roles, ownership, joins) keeps working.

Supabase owns: the credential store (`auth.users`), password hashing/verification,
access/refresh token issuing, and the confirmation / reset emails.

> Status: **implemented and building.** This document describes what actually ships in
> `com.kaptaitourist.kaptaitourist.supabaseauthmodule`, plus the dashboard/config it needs.

---

## 0. Design decisions (why it's built this way)

- **Mirror method.** A new user is written to **both** Supabase Auth *and* `khotel_user`.
  The two records are **linked by email**, *not* by a shared id — each side keeps its own
  id (local row = its own `UUID`; Supabase user = its own id). Email is the join key.
- **No local password.** `khotel_user.password_hash` is stored as `NULL` for Supabase
  users; Supabase owns the password. The column was made **nullable** (not dropped) so the
  old local-auth module keeps compiling until it is migrated later.
- **Atomicity — "if one side fails, both fail."** Achieved purely by **ordering** plus a
  local compensation, so **no `service_role` key is required**:
  1. validate input, 2. check local conflicts (email/mobile), 3. **insert the local row**,
  4. assign role + **call Supabase signup**. If anything after the local insert fails
  (Supabase down, duplicate in Supabase, etc.), the local row is **deleted** (compensation),
  so nothing is left behind. If the local insert itself fails, Supabase is never called.
- **Token only on login.** Register returns the created user with **no token**. The session
  (Supabase `access_token`) is returned by **login**.

---

## 1. Endpoints

Base path: **`/api/v1/supabase/auth`** — all four are **public** (no `Authorization`
header). Send `Content-Type: application/json`.

| Method | Path | Purpose | Success |
|---|---|---|---|
| POST | `/register` | mirror-create user (Supabase + `khotel_user`) | `201` user, **no token** |
| POST | `/login` | password grant against Supabase | `200` `access_token` + local roles |
| POST | `/forgotpassword` | trigger Supabase recovery email | `200` generic message |
| POST | `/resetpassword` | complete reset via emailed OTP code | `200` message |

Route constants live in `core/routes/RouteNames.java`; wiring is in
`adapter/in/web/router/AuthenticationRouterConfig.java`.

### 1.1 Register — `POST /register`
```json
{ "name": "Guest User", "email": "guest@example.com",
  "mobile": "+8801700000000", "gender": "MALE", "password": "Secret123!" }
```
`201 Created`:
```json
{ "message": "Registration successful",
  "userData": { "id": "<local-uuid>", "name": "Guest User",
    "email": "guest@example.com", "mobile": "+8801700000000", "gender": "MALE",
    "isActive": true, "roles": ["USER"], "version": 0,
    "createdBy": "supabase-registration", "createdAt": "..." } }
```
- Same validation/conflict rules as the legacy `UserService.register` (email/mobile
  format, gender `MALE|FEMALE`, password ≥ 6). Bad input → `400`; email or mobile already
  in `khotel_user` → `409`.
- `name`, `mobile`, `gender` are also written to the Supabase user's `user_metadata`.

### 1.2 Login — `POST /login`
```json
{ "email": "guest@example.com", "password": "Secret123!" }   // email OR mobile + password
```
`200 OK`:
```json
{ "token": "<supabase access_token>", "tokenType": "Bearer",
  "userId": "<local-uuid>", "email": "guest@example.com", "roles": ["USER"] }
```
- If `mobile` is supplied instead of `email`, the email is resolved from the local mirror
  (`findByMobile`) and used for the Supabase password grant.
- `roles` come from `khotel_user` (via `findByEmail`). `token` is the **Supabase** JWT.
- Errors: unconfirmed email → `401` "Please confirm your email before logging in.";
  wrong credentials → `401` "Invalid email or password".

### 1.3 Forgot password — `POST /forgotpassword`
```json
{ "email": "guest@example.com" }
```
`200 OK` (always, even for unknown emails — anti-enumeration):
```json
{ "message": "If an account exists for that email, a reset code has been sent." }
```

### 1.4 Reset password — `POST /resetpassword` (OTP-code flow)
```json
{ "email": "guest@example.com", "token": "123456", "newPassword": "NewSecret456!" }
```
`200 OK`:
```json
{ "message": "Password has been reset. You can now log in with your new password." }
```
- `token` is the one-time **code** from the recovery email — this requires the *Reset
  Password* email template to include `{{ .Token }}` (see §4). Bad/expired code → `400`.
- Passwords live only in Supabase, so reset does **not** touch `khotel_user`.

---

## 2. How it maps to GoTrue (`{SUPABASE_URL}/auth/v1`)

Every call carries `apikey: <anon>` and `Authorization: Bearer <anon>` (the anon key is
enough for all four flows). Implemented in `adapter/out/supabase/SupabaseAuthClient.java`.

| Our endpoint | GoTrue call |
|---|---|
| register | `POST /signup` — body `{ email, password, data:{name,mobile,gender} }` |
| login | `POST /token?grant_type=password` — body `{ email, password }` |
| forgotpassword | `POST /recover` — body `{ email }` |
| resetpassword | `POST /verify` `{ type:"recovery", email, token }` → then `PUT /user` with `Authorization: Bearer <recovery access_token>` body `{ password }` |

**Error translation** (GoTrue non-2xx → our exceptions → `GlobalExceptionHandler`):
`invalid_grant` → `InvalidCredentialsException` (401); body contains "not confirmed" →
401 with a clear message; signup "already/exists/registered" → `ConflictException` (409);
verify failure → `ValidationException` (400); anything else → 500.

---

## 3. RBAC — the four routes must be seeded PUBLIC

Authorization is **data-driven** (`RbacFilter` → `PermissionService`), matching the request
method + path against rows in the `permission` table. A row with `permission_name = 'ALL'`
marks an endpoint public. **`SecurityConfig` permitting the path is not enough** — without
a permission row, `RbacFilter` returns `401 "Authentication required"`.

`RbacFilter` authorizes the **raw request path before routing**, so you must hit the full
action path (e.g. `/api/v1/supabase/auth/register`), not the base `/api/v1/supabase/auth`.

Seeded by `db/changelog/seed-supabase-auth-permissions.sql` (4 rows, `permission_name='ALL'`,
`service_name='supabaseauth'`), guarded by `NOT EXISTS` so it is re-run safe. Verify with:
```sql
SELECT url, method FROM permission
WHERE permission_name='ALL' AND url LIKE '/api/v1/supabase/auth%';
```

---

## 4. Supabase dashboard configuration

| Setting | Location | Value |
|---|---|---|
| Email provider | Authentication → Providers → Email | **enabled** |
| Allow new users to sign up | Authentication → Providers → Email | **ON** |
| **Confirm email** | Authentication → Providers → Email | **OFF for dev testing** (see below); **ON** for prod |
| Reset Password template | Authentication → Email Templates → Reset Password | must include `{{ .Token }}` for the OTP reset flow |

**Confirm email toggle — what it changes:**
- **OFF** → `signup` returns a usable, already-confirmed user; you can register **and log
  in immediately** with throwaway emails (no inbox needed). Best for backend/Postman testing.
- **ON** → the new user is *unconfirmed* and **login is blocked** until the confirmation
  link is clicked. Register still works (row created on both sides); login returns
  `401 "Please confirm your email before logging in."` until confirmed. To test login with
  Confirm-email ON you must use a real inbox, or manually confirm the user under
  Authentication → Users, or set it OFF.

---

## 5. Config / environment

Reuses the existing Storage properties — **no new env var, no `service_role` key**:
```properties
supabase.url=${SUPABASE.URL}
supabase.anon-key=${SUPABASE.ANON.KEY}
```
`SupabaseAuthClient` builds a `WebClient` against `${supabase.url}/auth/v1` with the anon
key as both `apikey` and `Authorization` (the `PUT /user` step overrides `Authorization`
with the per-request recovery token).

---

## 6. Files in this module

```
supabaseauthmodule/
  adapter/in/web/
    dto/            RegisterRequestDto, LoginRequestDto, AuthResponseDto, UserResponseDto,
                    ForgotPasswordRequestDto, ResetPasswordRequestDto, MessageResponse
    handler/        AuthenticationRouterHandler   (register, login, forgotPassword, resetPassword)
    router/         AuthenticationRouterConfig
  application/
    port/in/        SupabaseUserUseCase
    port/out/       SupabaseAuthPort
    service/        SupabaseAuthService           (mirror + atomicity orchestration)
  adapter/out/supabase/
    SupabaseAuthClient                            (GoTrue WebClient)
    dto/            SupabaseUser, SupabaseSession
```
Reused from the `user` module: `UserPort` (added `deleteById` for compensation),
`User`, `Gender`. DB changesets: `alter-user-password-nullable.sql`,
`seed-supabase-auth-permissions.sql` (both registered in `db.changelog-master.yaml`).

---

## 7. Repeat-testing notes (dev)

- **Email must be unique.** Re-registering the same address → `409` from the local mirror
  check. Use a fresh email per test, or delete the previous user from **both** the Supabase
  dashboard (Authentication → Users) **and** `khotel_user` before retrying.
- With **Confirm email OFF**, `test1@example.com`, `test2@example.com`, … work end-to-end
  with no real inbox.

---

## 8. Deferred (not in this iteration)

- Migrating the old `user` / `ownerrequest` modules and `AdminSeeder` off local bcrypt+JWT.
- Making `SecurityConfig` / `JwtService` **validate the Supabase JWT** so protected routes
  accept the login `token` (the "swap the Authorization header to the Supabase token" work).
  Until then the Supabase `token` authenticates *these* flows but is not yet accepted by the
  app's other protected endpoints.
- Dropping the `password_hash` column outright.
- A self-service authenticated change-password endpoint.

---

## 9. Security reminders

- Never commit `.env.image`. The `anon` key is designed to be public (it's the `apikey`).
- No `service_role` key is used here; if one is added later for admin ops, it bypasses RLS
  — keep it server-only.
- Keep `recover` enumeration-safe (always the same generic 200 response).
