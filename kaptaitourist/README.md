# Kaptai Tourist — Reactive Hotel Booking Platform (Backend)

A production-style, fully **reactive** multi-tenant hotel-booking backend built on **Spring Boot 4 / WebFlux** and **R2DBC**, with a **data-driven RBAC + resource-ownership** authorization engine, **date-aware availability with an oversell guard**, and a reactive **image-processing pipeline** to object storage.

Built end-to-end with a **hexagonal (ports & adapters)** architecture and **functional routing** — no `@RestController`s, no blocking calls on the event loop.

> Single-developer project. ~10 feature modules · 54 HTTP endpoints · ~15 database tables · non-blocking top to bottom.

---

## Highlights (the interesting engineering)

- **Data-driven authorization engine.** Every endpoint's access rule lives in the database (`role` → `role_permission` → `permission`), not in code. A single `WebFilter` matches each request's method + URL template against the permission table and grants/denies — 67 seeded endpoint rules, changeable without a redeploy.
- **Resource-level ownership on top of RBAC.** A `requires_ownership` flag on a permission makes the filter additionally verify the caller **owns the `{hotelId}`** in the path (extracted from the matched URL template) — so a hotel owner manages only their own hotels, while ADMIN bypasses. Role check is stateless (from the JWT); ownership is checked live against the DB.
- **Correct, race-safe booking.** Availability is **computed, never stored** (`total_units − Σ overlapping non-cancelled units`). Booking creation runs in a reactive `@Transactional` flow that locks the room row `FOR UPDATE`, so concurrent bookings can't oversell.
- **Reactive image pipeline.** Multipart upload → **magic-byte format detection** (not header-trust) → EXIF-correct rotate/resize/thumbnail (Thumbnailator + TwelveMonkeys) → **Supabase Storage** REST — with the blocking image work offloaded to a bounded-elastic scheduler so the Netty event loop stays free.
- **Rich query layer.** Public hotel search combines **name + facility (ALL-match) + date-availability + pagination**, built as dynamic SQL via `DatabaseClient` because the filter combinations don't fit static queries.
- **Owner & admin dashboards.** Live rollups (occupancy tonight, upcoming bookings, revenue, facilities, media) computed with a handful of grouped aggregate queries — no N+1.
- **Owner enlistment workflow.** Self-service `register-owner` files a request; an admin approves (granting the role) or rejects, with terminal-state guarding.

---

## Tech stack

| Area | Technology |
|---|---|
| Language / Runtime | **Java 21** |
| Framework | **Spring Boot 4.1**, **Spring WebFlux** (reactive, functional `RouterFunction` routing) |
| Reactive core | **Project Reactor** (`Mono`/`Flux`) — non-blocking end to end |
| Persistence | **Spring Data R2DBC** (reactive Postgres) + `DatabaseClient` for dynamic/aggregate SQL |
| Database | **PostgreSQL** (hosted via **Supabase**), pooled connections |
| Migrations | **Liquibase** (JDBC) — versioned changelog |
| Security | **Spring Security 7** reactive chain + **JWT** (jjwt, HS256) + **BCrypt**; custom data-driven RBAC & ownership `WebFilter` |
| Object storage | **Supabase Storage** (REST via reactive `WebClient`) |
| Image processing | **Thumbnailator** + **TwelveMonkeys ImageIO**, magic-byte sniffing, EXIF orientation |
| Mapping | **ModelMapper** (entity ↔ domain) |
| Build | **Gradle** (toolchain-pinned JDK 21) |
| API docs | **OpenAPI 3** spec (`doc/openapi.yaml`) |

---

## Architecture

Hexagonal / ports-and-adapters, one module per bounded context:

```
hotel/  room/  booking/  facility/  image/  user/  dashboard/  ownerrequest/  welcome/  core/
  ├─ adapter/in/web/    → functional routers + handlers + request/response DTOs
  ├─ application/
  │   ├─ port/in        → use-case interfaces
  │   ├─ port/out       → persistence/storage ports
  │   └─ service        → business logic (implements use cases)
  ├─ adapter/out/       → R2DBC repositories + entities (implements out-ports)
  └─ domain/            → framework-free domain models
```

`core/` holds the cross-cutting machinery: the RBAC `WebFilter`, JWT security config, ownership checker, global reactive exception handling, and route constants.

**Request flow:** `RouterFunction` → JWT authentication filter → data-driven `RbacFilter` (role + ownership) → handler → use-case service → out-port → R2DBC/Storage — all `Mono`/`Flux`, no blocking hops.

---

## Feature domains

| Module | Capability |
|---|---|
| **Hotel** | CRUD, public search (name/facility/date/pagination), enriched detail (images + rooms + facilities + cover image), owner & admin list views |
| **Room** | Room-*type* model with unit inventory, nested under a hotel, embedded galleries + facilities |
| **Booking** | Date-aware availability, transactional oversell-safe create, cancel, per-hotel management |
| **Facility** | Global catalog + per-hotel/room assignment with charge/complimentary/availability qualifiers and per-creator ownership |
| **Image** | Hotel & room galleries → Supabase Storage; upload/replace/delete; set-primary cover; cascade cleanup |
| **User / Auth** | Register, login by **email or mobile**, JWT issue, masked profile, change-password, admin promote |
| **Owner requests** | Self-register-as-owner → admin approve/reject enlistment workflow |
| **Dashboard** | Per-hotel (owner) and platform-wide (admin) live aggregate rollups |

---

## Getting started

**Prerequisites:** JDK 21, a PostgreSQL database (e.g. a free Supabase project), and a Supabase Storage bucket.

1. **Configure** — provide secrets via a `.env` at the repo root (imported through `spring.config.import`):

   ```
   SPRING.R2DBC.URL / USERNAME / PASSWORD        # reactive DB (pooler)
   SPRING.LIQUIBASE.URL / USER / PASSWORD        # JDBC DB (migrations)
   SUPABASE.URL / SUPABASE.ANON.KEY / SUPABASE.STORAGE.BUCKET
   APP.JWT.SECRET (>= 32 chars) / APP.JWT.EXPIRATION_MS
   APP.ADMIN.EMAIL / PASSWORD / NAME / MOBILE    # bootstrap admin
   ```

2. **Run** — Liquibase applies the schema on startup; a bootstrap ADMIN is seeded.

   ```bash
   ./gradlew bootRun
   ```

   Service listens on **`http://localhost:9090`**, base path **`/api/v1`**.

3. **Smoke test:**

   ```bash
   curl http://localhost:9090/api/v1/welcome -H "Accept: application/json"
   ```

---

## API overview

Full machine-readable spec: **`doc/openapi.yaml`**. A living architecture snapshot lives in **`doc/project.md`**, and an engineering log (SQA findings, fixes, roadmap) in **`doc/issues.md`**.

- **Public:** browse & search hotels/rooms, check availability, view facilities, register, login.
- **USER:** book a room, manage own profile & password.
- **HOTEL_OWNER:** manage their own hotels, rooms, images, facility assignments, bookings, and dashboard.
- **ADMIN:** full platform control — user management, catalog writes, owner-request review, platform dashboard.

Auth is `Authorization: Bearer <jwt>`; roles are carried in the token, ownership is verified per request.

---

## Notes

This repository is the **backend service**. It was also ported to a wire-compatible **NestJS** implementation as an exercise in re-expressing the same domain and contracts on a different stack.
