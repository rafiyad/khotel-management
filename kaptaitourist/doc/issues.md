# Known Issues / Deferred

Tracked issues that are understood but intentionally **not fixed yet**.

---

## 1. Room images leak to USER via the room embed

**Status:** Open — deferred (2026-06-26)
**Area:** Authorization / Room + Image modules

**Context:** Facility and image endpoints were locked down to HOTEL_OWNER/ADMIN (USER and
anonymous get 403/401 on `/facility/**`, `/image/**`, and the hotel/room facility + image
endpoints).

**The gap:** `GET /hotel/{hotelId}/room/{roomId}` and the room list `GET /hotel/{hotelId}/room`
are **public** (so visitors can browse rooms) and their responses **embed the room's images**
(`Room.images` — populated from `khotel_attachment`). So a USER (or anonymous) still sees image
**URLs** in the room payload, even though the dedicated `/image/...` endpoints are now blocked.

**Why it matters:** "images hidden from USER" is only partially true — the dedicated endpoints
are blocked, but the embed still exposes the URLs. If the bucket is/becomes public, those URLs
are directly fetchable.

**Possible fix (when picked up):** strip `images` from the room response for callers who are not
the hotel's owner or an admin (i.e. only embed images for owner/admin reads), or stop embedding
images on public room reads and require the dedicated (now-protected) image endpoints.

**Decision:** Left as-is for now per product call — room photos are arguably desirable for
visitors browsing rooms. Revisit if images must be fully owner/admin-only.



## 2. new (bug) need to fix next time

1. Jpeg format showing 500 error with no reader found  → ✅ FIXED, see SQA #8 below
2. hotel search don't include image in its profile     → see SQA #35 below (isPrimary unreachable)

## 3. Add simple logback with the end point and time of the request to the log file

## 4. Add search by hotel name  → IMPLEMENTED 2026-07-01 (see git; `GET /hotel?search=`)

---

# SQA Review Register — 2026-07-01

Static code review by three parallel review passes (hotel+room, image+facility, auth+security)
plus a manual booking/RBAC/config pass. **The app was not run** (per the "never run" rule), so
nothing below is runtime-verified — a dynamic pass is the natural follow-up. Findings grouped by
severity. Tags: **[BUG]** defect · **[SEC]** security · **[VAL]** missing validation ·
**[PERF]** performance · **[OPS]** operational gotcha · **[FEAT]** missing feature.
Each entry: what's wrong → concrete failure scenario → `file:line`.

## 🔴 Critical / High

### 5. [BUG] Deleting a room/hotel silently destroys confirmed bookings
`deleteRoom`/`deleteHotel` never check for active bookings, and `khotel_booking` has
`ON DELETE CASCADE`. Owner deletes a room type with 5 confirmed future bookings → guests'
reservations vanish with no cancellation, refund, or record.
`RoomService.java:150`, `HotelService.java:171`

### 6. [BUG] Room PUT resets omitted fields to hardcoded defaults
`updateRoom` doesn't merge (unlike `HotelService`) — omitted fields become capacity=1,
totalUnits=1, price=0, discount=0, isAvailable=true, type/description=null. Client PUTs
`{"roomName":"Premium","pricePerNight":120}` to change a price → inventory drops 7→1 units and
description is wiped. `RoomService.java:122-130`

### 7. [VAL] totalUnits can be reduced below already-booked units → physical overbooking
`updateRoom` doesn't compare new `totalUnits` against existing bookings. Room has 7 units, 5
booked next week; owner sets totalUnits=2 → availability goes negative, confirmed guests
overbooked. `RoomService.java:109-137`

### 8. [BUG] JPEG upload → 500 "no reader found" (user-reported) — ✅ FIXED 2026-07-01
Three converging causes: (1) validation trusted the **Content-Type header only**, so a HEIC
photo renamed `.jpg` arrived as `image/jpeg`, skipped heif-convert, hit ImageIO which has no HEIF
reader; (2) `build.gradle` had `imageio-core` but **not `imageio-jpeg`** — core registers zero
readers, so CMYK/Adobe JPEGs failed even in the JPEG path; (3) the `UnsupportedFormatException`
wasn't mapped → raw 500 instead of 400.
**Fix (all three):** added `imageio-jpeg:3.13.1` to `build.gradle`; `ImageUtil.detectImageType()`
now identifies the real format from **magic bytes** (JPEG/PNG/WEBP/HEIF signatures) instead of
the header, routing renamed-HEIC to conversion and rejecting non-images with a 400; Thumbnailator
decode failures in `compress()` are caught and rethrown as `ValidationException` → 400.
**Remaining:** HEIC still requires the `heif-convert` binary on the host (now fails as a clean
400/processing error, not a misleading 500). Not yet runtime-verified. `ImageUtil.java`, `build.gradle`

### 9. [SEC] Uploads trust client Content-Type — arbitrary files in the public bucket — 🟡 MITIGATED 2026-07-01
WebP passed through byte-for-byte, header-only validation. Upload an `.exe` or HTML/XSS polyglot
as `Content-Type: image/webp` → stored verbatim in the public bucket and served as-is.
**Mitigated by the #8 fix:** `ImageUtil.detectImageType()` now validates real magic bytes before
processing, so a non-image labelled `image/webp` is rejected with a 400 and never reaches storage.
**Still open:** detection covers the first 12 bytes only — a valid-image-header polyglot (real
JPEG/WEBP magic bytes with appended malicious payload) would still pass; and the bucket remains
public. Consider re-encoding all formats (WebP is still pass-through, not decoded) and/or signed
URLs. `ImageUtil.java:detectImageType`

### 10. [BUG] updateImage silently converts room images into hotel-gallery images
`updateImage` rebuilds the entity without `.roomId(existing.getRoomId())` → PUT on a room image
NULLs `room_id`, moving it to the hotel gallery and corrupting both display_order sequences.
`ImageService.java:137-152`

### 11. [SEC] Committed fallback secrets + dev profile active
`application-dev.properties` (in git) ships a working JWT secret
(`dev-secret-change-me-...`) and admin `admin@kaptai.local` / `Admin@12345`, and
`application.properties` sets `spring.profiles.active=dev`. If `APP.JWT.SECRET` is unset in a
deployment, anyone who knows the default can **forge ADMIN tokens**. Should fail-fast, not
start insecure. `application-dev.properties:31,35`

### 12. [SEC] Ownership enforcement — ✅ RE-ENABLED 2026-07-01
Previously dropped in the move to pure role-based RBAC, so any HOTEL_OWNER could manage any hotel.
**Re-enabled** as a resource-level layer on top of RBAC: the `permission` table gained a
`requires_ownership` flag; for flagged endpoints, `RbacFilter` extracts `{hotelId}` from the matched
permission's URL template and requires the caller to own it (`OwnershipChecker` → `khotel_hotel_owner`),
**ADMIN bypassing**. Owner-scoped: hotel/room update+delete, all image endpoints, facility
assignment writes, booking list/get/cancel, and the dashboard. `POST /hotel` and `POST .../booking`
are intentionally not ownership-scoped (create-hotel records the creator as owner; guests book any
hotel). `RbacFilter`, `PermissionService.authorize`, `create-rbac-tables.sql`.

### 13. [SEC] `passwordHash` could leak via /auth/me and GET /user — ✅ FIXED 2026-07-01
`User.passwordHash` was protected only by a `com.fasterxml.jackson.annotation.@JsonIgnore`, but
the runtime classpath confirms **both** Jackson stacks are present and Boot 4's WebFlux codec is
**Jackson 3** (`tools.jackson:jackson-databind:3.1.4`) — so annotation recognition was uncertain
for a BCrypt-hash field returned by `/auth/me` and `GET /user` (which wrap the domain `User`).
**Fix:** rather than trust the annotation, the hash is now stripped at the domain→response
boundary — `UserService.withoutSecret()` nulls `passwordHash` on every user handed to the web
layer (register, `/auth/me`, `/user` list, promote). Guarantees no hash serializes regardless of
Jackson version; the `@JsonIgnore` is kept as a second line of defence. Login is untouched (it
needs the hash internally and never returns the user). `/auth/profile` was already safe (ProfileDto).
**Follow-up (not done):** the proper long-term shape is a dedicated response DTO that has no
password field at all, instead of serializing the domain entity. `UserService.java:withoutSecret`

### 14. [SEC] Stale roles/deactivation live in JWTs until expiry (24h)
Roles baked at login; promote/demote/deactivate take effect only after the 24h token expires.
No refresh/blacklist; `is_active` is not re-checked per request. `JwtService.java`, `UserService.java:110`

### 15. [FEAT] A guest cannot see or cancel their own booking
`POST .../booking` is USER-allowed, but list/get/cancel are owner/admin-only and there's no
`/my-bookings`. A guest books then has no way to view or cancel it.

## 🟠 Medium

### 16. [SEC] PermissionService swallows DB errors as 403 with no logging
`onErrorResume(t -> Mono.just(false))` — a DB outage becomes a silent 401/403 storm with zero
logs, defeating alerting. Fail-closed is right; silent is not. `PermissionService.java:54`

### 17. [PERF] RBAC hits the DB on every request, uncached
Up to 2 queries per request (public check + role join), including on public endpoints, for
effectively-static reference data. DoS/throughput surface; amplifies #16. Needs an in-memory
cache with refresh. `PermissionService.java:34-52`

### 18. [BUG] Multi-file upload has no transactionality/compensation
Concurrent upload+save; file 3 of 5 fails → files 1–2 already in storage+DB (request still
errors); storage-success/DB-fail orphans an object; retry duplicates survivors.
`ImageService.java:58-70`

### 19. [BUG] updateImage deletes old storage files BEFORE the DB save
If the save then fails (optimistic lock), the row points at deleted files — broken image,
orphaned replacement. `ImageService.java:127-154`

### 20. [BUG] Concurrent uploads produce duplicate display_order
Order derived from a pre-read `count()`, no unique constraint — two uploads both read N. Silent
corruption. `ImageService.java:53-58`

### 21. [BUG] Storage delete failures swallowed in SupabaseStorageService
`onErrorResume → Mono.empty` inside the storage service makes ImageService's compensation dead
code; DB rows always deleted even when bucket delete fails → orphans accumulate (log only).
`SupabaseStorageService.java:110-133`

### 22. [BUG] Optimistic-lock conflicts surface as 500, not 409
No branch for `OptimisticLockingFailureException` (not a `DataIntegrityViolationException`). Two
admins edit the same hotel → loser gets a 500 with a raw message. `GlobalExceptionHandler.java:20-56`

### 23. [BUG] createdBy/updatedBy client-spoofable in Room and Facility
Both take audit fields from the request DTO (Image module correctly uses the token). Any caller
writes `createdBy:"admin"` or leaves it null. `RoomService.java:60,134`, `FacilityService.java:48,100`

### 24. [BUG] 500 leaks raw exception messages + printStackTrace to stdout
Catch-all returns `ex.getMessage()` as JSON and calls `ex.printStackTrace()` — internal DB/driver
detail leaks to clients; traces bypass SLF4J. `GlobalExceptionHandler.java:54,93-98`

### 25. [BUG] JSON naming asymmetry breaks GET→PUT round-trips for room booleans
Domain serializes `airConditioned`/`available` (Lombok primitive getters) but the DTO only
reads `isAirConditioned`/`isAvailable`; round-tripping a GET into a PUT ignores both keys, and
with #6 the flags silently reset. `Room.java` vs `RoomRequestDto.java`

### 26. [BUG] Room-scoped image routes don't verify the room belongs to the path's hotel
`{hotelId}` in path but handler passes only roomId; becomes a real cross-hotel bypass the day
ownership returns. `ImageHandler.java:179-201`

### 27. [SEC] No path canonicalization before RBAC matching
`RbacFilter`/`PermissionService` match `getURI().getPath()` verbatim via AntPathMatcher — no
`cleanPath`. Trailing-slash, `//api/v1/user`, matrix-param, or encoding variants are a potential
bypass surface (protected handler reached via a permissive public pattern, or vice versa).
`RbacFilter.java:43`, `PermissionService.java:36`

### 28. [SEC] User enumeration + no mobile normalization on register
Distinct 409 messages ("Email is already registered" vs "Mobile number is already registered")
let an attacker enumerate accounts; sequential checks add a timing signal. Also mobile isn't
normalized, so `+8801…` and `01…` create duplicate accounts for one number, bypassing the UNIQUE
constraint. `UserService.java:58-70`

### 29. [BUG] AdminSeeder uses `.block()` on the reactive/Netty context
Blocking a reactive pipeline at startup can deadlock/throw on a non-blocking thread; and
`onErrorResume(e -> Mono.empty())` swallows seed failures → system may boot with no admin and no
hard error. `AdminSeeder.java:46-50`

### 30. [OPS] Single-filter authz with no defense-in-depth
100% of authorization lives in one `LOWEST_PRECEDENCE` WebFilter while `authorizeExchange` is
`permitAll`. Any future WebFilter that registers lower or short-circuits bypasses authz; also
OPTIONS is unconditionally passed through (fine for CORS, latent if a route ever answers OPTIONS
with data). `SecurityConfig.java:73`, `RbacFilter.java:43`

### 31. [VAL] No numeric validation on room create/update
Negative price/discount, discount > price, capacity < 1, totalUnits ≤ 0 accepted (no DDL CHECKs).
price=100/discount=500 → negative booking totals. `RoomService.java:39-63,109-137`

### 32. [VAL] numberOfGuests never validated on booking
Not checked > 0 nor against `capacity × units`. 1 unit, 50 guests → accepted. `BookingService.java:38-49`

### 33. [BUG] `discount` semantics ambiguous — treated as absolute per-night amount
`computeTotal` does `pricePerNight − discount`; if intended as a percentage, every total is
wrong. Needs a product decision + docs. `BookingService.java:168-174`

### 34. [FEAT] GET /hotel scaling (pagination + N+1) — ✅ RESOLVED 2026-07-01
- ✅ **Pagination — ADDED 2026-07-01.** `GET /hotel` now takes `page`/`size` (default 0/10, size
  capped at 100) via `findPage`/`countPage`; response carries `page`, `size`, `totalRecords`,
  `totalPages`. Enrichment is now bounded to **one page** of hotels, not the whole table — this
  removes the "whole dataset per request" problem and caps the per-hotel fan-out at page size.
- ✅ **Facility filter — ADDED 2026-07-01.** `?facility=<id>` (repeatable) does an ALL-match on
  hotel-level facilities via a `GROUP BY … HAVING COUNT(DISTINCT)=n` subquery; combines with
  `search` (AND).
- ✅ **Per-room facility N+1 — FIXED.** Batched via `facilityPort.findRoomFacilitiesByRoomIds`
  (2 queries: links `IN` + facilities `IN`, grouped by roomId). Applied in `HotelService.enrich`
  and `RoomService.attachDetails`. (The old `findRoomFacilities` was itself N+1 — `findById` per link.)
- ✅ **Per-room image N+1 — FIXED.** `enrich` batches room images via `imagePort.findAllByRoomIdIn`.
- 🟠 **Minor remaining:** within a page, each hotel is still enriched by its own call (~4 queries
  per hotel on the page). Bounded by `size` now, so not a scaling risk; a batched multi-hotel
  enrichment (incl. `findHotelFacilitiesByHotelIds`) is an optional future optimization. `HotelService.java`

### 35. [FEAT] isPrimary is unreachable — no set-primary endpoint — ✅ FIXED 2026-07-04
Was hard-coded false with no way to mark an image primary. **Fixed:** `POST /image/{hotelId}/{imageId}/primary`
and `POST /hotel/{hotelId}/room/{roomId}/image/{imageId}/primary` (owner-scoped) — transactional
unset-then-set within the gallery, so `coverImageUrl` now reflects an actual chosen cover.
`ImageService.setHotelImagePrimary/setRoomImagePrimary`.

### 36. [FEAT] No API to manage the DB-driven RBAC
`role`/`permission`/`role_permission` change only via hand-written SQL. Forgetting a permission
row for a new endpoint makes it dead-on-arrival (deny-by-default). No admin CRUD for the matrix.

## 🟡 Low

### 37. [BUG] Nonexistent parents return 200-empty instead of 404
`GET /hotel/{bogus}/room` and `/hotel/{bogus}/booking` don't verify the hotel exists.
`RoomService.findAllByHotelId`, `BookingService.findAllByHotelId`

### 38. [BUG] Missing principal mapped to 400 instead of 401 in Hotel/Booking handlers.

### 39. [VAL] No string-length validation → over-length values throw DataIntegrityViolation, mapped
to the **misleading 409** "same unique value already exists" (e.g. 300-char hotel name).

### 40. [VAL] Hotel contact fields (email/mobile/website/googleMapUrl) unvalidated; booking guest
email/phone unvalidated; no cap on stay length or booking horizon.

### 41. [VAL] Facility link data unvalidated — negative `additionalCharge`; contradictory
`isComplimentary=true` + charge > 0 persisted (adapter defaults null complimentary to true while
keeping the charge). `updateFacility` can flip `appliesTo` stranding existing assignments.

### 42. [VAL] Empty/0-byte uploads pass validation (no magic-byte/empty check) → 500 in
Thumbnailator instead of 400. Unused `sanitize()` shows sniffing was planned, never wired.

### 43. [BUG] Cancelling a past/completed stay is allowed — no cutoff in `cancelBooking`.

### 44. [BUG] Dead API field — `Room.hotelName` is in responses but never populated (always null).

### 45. [BUG] `extractRole` in UserContextService strips a "ROLE_" prefix that's never applied and
picks `findFirst()` with no ordering → non-deterministic audit label for multi-role users; still
has TODO comments. `UserContextService.java:41-46`

### 46. [OPS] Deny-by-default side effects: unknown paths return 401/403 (never 404); every new
endpoint needs a seeded permission row; role names duplicated between `khotel_role` and `role`
must stay in sync.

### 47. [SEC] No clock-skew leeway on JWT parse — minor issuer/validator drift rejects valid
tokens; no issuer/audience validation (alg=none and expiry are handled by jjwt 0.12.6).
`JwtService.java:40`

### 48. [FEAT] Product/API gaps (roundup)
- No pagination anywhere; no PATCH (hotel's null-means-keep merge can never clear a field).
- Search now exists for hotel **name**; no filter by price/capacity/date availability.
- No image reorder endpoint; displayOrder not client-settable.
- Auth: no logout/refresh/revocation, no password change/reset, no email verification, no demote
  endpoint, no login rate-limiting / account lockout.
- Facility catalog list unpaginated.
- Supabase bucket must be public for current URLs (or move to signed URLs) — still open.

---

## How this was produced / what's still pending

- **Static only** — app not run; the above is not runtime-verified. Dynamic follow-up: curl the
  RBAC matrix, 401/403 bodies, booking oversell race, upload edge cases against a running instance.
- **#13 (passwordHash leak) needs a live check** against the configured WebFlux JSON codec — it's
  the one finding that could be either harmless or critical depending on Jackson 2 vs 3.
- Verified non-issues: room update/delete scoped by `findByIdAndHotelId`; image cleanup ordering
  on delete; Persistable/isNew UUID pattern; facility duplicate-name → 409; register race → 409;
  object keys are server-generated UUIDs (no path traversal); Thumbnailator offloaded to
  boundedElastic; jjwt rejects alg=none and checks expiry.

---

# Missing Features / Roadmap — 2026-07-01

Product/feature gaps for a real hotel-booking platform (distinct from the bug/security register
above). Prioritized by impact. Nothing here is started unless noted. Existing SQA cross-refs in
parentheses.

## 🔴 Blocking for a shippable booking product

### R1. Payments
No payment handling at all — a booking is created but nothing is charged (no deposit, capture,
or refund on cancel). **Needs:** gateway integration (Stripe / SSLCommerz / bKash for BD), a
`payment` table tied to bookings, and a payment status on the booking (PENDING → PAID → REFUNDED).
Architecturally significant (new module + external integration). *The single biggest gap.*

### R2. Cross-hotel "available for my dates" search — ✅ DONE 2026-07-01
`GET /hotel?checkIn&checkOut&guests` now filters to hotels with ≥1 room that has a free unit
across the range (`total_units − SUM(overlapping non-cancelled units) >= 1`) and `capacity >=
guests`. Combines (AND) with `search` + `facility` + pagination. Because name × facility × date
combinations don't fit static `@Query` methods, hotel search was refactored to **dynamic SQL via
`DatabaseClient`** (`HotelAdapter.buildFilter` + `HotelSearchCriteria`); the old static repository
queries were removed. Public. `HotelAdapter`, `HotelSearchCriteria`, `HotelService.findAll`.

### R3. Guest "my bookings" + self-cancel  (see SQA #15)
A USER can book but cannot see or cancel their own reservations (list/get/cancel are owner/admin
only). **Needs:** `GET /me/bookings`, `GET /me/bookings/{id}`, `POST /me/bookings/{id}/cancel`,
scoped to `booking.user_id = caller`. New permission rows (user-owns-booking check, analogous to
hotel ownership).

## 🟠 Important — expected on any booking site

### R4. Notifications
No booking confirmation / cancellation email or SMS, no check-in reminders. **Needs:** an outbound
notification service (email/SMS provider) triggered on booking create/cancel.

### R5. Reviews & ratings
No review model. Social proof drives bookings. **Needs:** a `review` table (guest → hotel, rating +
text, ideally gated to completed stays) + aggregate rating on the hotel response.

### R6. Cancellation policy + refund rules  (see SQA #43)
Cancel is currently free/instant with no cutoff. **Needs:** policy modeling (free-until-Nh,
partial/non-refundable rates) and a cancel cutoff enforced in `cancelBooking`.

### R7. Auth lifecycle  (see SQA #48) — 🟡 PARTIAL
- ✅ **Change password — DONE 2026-07-01.** `POST /auth/change-password` (any authenticated user)
  verifies `currentPassword` (BCrypt), then sets `newPassword` (≥6, must differ). New
  `AUTH.CHANGE_PASSWORD` permission granted to USER/HOTEL_OWNER/ADMIN. `UserService.changePassword`.
- ⬜ **Still missing:** password **reset**/forgot (needs email → depends on R4 notifications), email
  verification, logout/refresh + token revocation (SQA #14 — change-password does NOT invalidate
  existing JWTs), login rate-limiting / account lockout.

### R8. Pricing depth
Flat `pricePerNight − discount` only — no taxes/fees breakdown, no promo/coupon codes, no
seasonal rate plans. Minimum: tax + service fee on the total. (Also resolve the `discount`
semantics ambiguity, SQA #33.)

### R9. Location search
Address + `googleMapUrl` are stored but not searchable. **Needs:** city/area field + filter (and
optionally geo radius). Most hotel search starts with "where".

### R10. Booking modification
Only create/cancel exist — no "change dates / guests / room".

## 🟡 Hardening & correctness (several already tracked above)

- **R11. Idempotency on booking create** — no idempotency key; a double-submit/retry can create two
  bookings. Cheap, prevents real double-bookings.
- **R12. Secrets management** (SQA #11), **input validation gaps** (SQA #31/#32/#39–#42),
  **rate limiting**, **observability / request trace-ids** (correlation id filter — deferred).
- **R13. Admin platform-wide dashboard** — ✅ DONE 2026-07-01. `GET /admin/dashboard` (ADMIN only)
  returns cross-hotel totals: hotels/owners/users/facilities, rooms booked/available tonight +
  occupancy, upcoming bookings + today's arrivals/departures + cancellations, upcoming revenue —
  3 aggregate queries via `DashboardAdapter.loadPlatform`. New `ADMIN_DASHBOARD.GET` permission
  (ADMIN only; excluded from HOTEL_OWNER).
- **R14. Facility filter is not availability-aware** — `GET /hotel?facility=` matches assigned
  facilities even if `is_available = false`. One-line `WHERE` change if desired.

## Suggested sequencing (MVP loop: browse → book → pay → confirm → manage)
**R2 (date search) → R3 (my-bookings + self-cancel) → R1 (payments) → R4 (confirmation
notifications)**, then R6 (cancellation policy), R5 (reviews), R8 (pricing). R1 and R2 are the
architecturally significant ones; the rest are incremental.

---

# Ported from the NestJS service — 2026-07-04

Endpoint/behaviour improvements brought over from the wire-compatible NestJS port
(`~/personal-projects/hotel-management`). All compiled-verified only.

- ✅ **GET /welcome** — public smoke test (`{message, status}`), no auth/DB.
- ✅ **Login by email OR mobile** — `POST /auth/login` accepts `{email}` or `{mobile}` + password.
- ✅ **Set-primary image** — hotel + room cover endpoints (see #35 above).
- ✅ **Owner enlistment workflow** — `POST /auth/register-owner` (public: creates USER + PENDING
  request), `GET /owner-request?status=`, `POST /owner-request/{id}/approve|reject` (ADMIN;
  approve grants HOTEL_OWNER; decided requests → 409). New `khotel_owner_request` table.
- ✅ **Owner/Admin hotel lists** — `GET /owner/hotel` (caller's own hotels, enriched);
  `GET /admin/hotel` (all hotels + owners + room-type/booking counts, no image enrichment).
- ✅ **Facility ownership** — `created_by_id` on `khotel_facility` (from token); create is now
  HOTEL_OWNER/ADMIN; update/delete: ADMIN any, HOTEL_OWNER only their own (else 403).

**Not ported (per user selection):** guest my-bookings + self-cancel (roadmap R3) — still open.

**Notes / caveats carried over:**
- Approve/reject grant the role in `khotel_role`, but the requester must **re-login** to get a
  token carrying HOTEL_OWNER (stale-JWT, SQA #14).
- `created_by_id` is a **new** column, kept distinct from the existing free-text audit `created_by`.
- Schema reset required: new `khotel_owner_request` table, `khotel_facility.created_by_id`, and
  many new RBAC permission rows.
