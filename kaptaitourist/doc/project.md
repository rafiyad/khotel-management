# Kaptai Tourist — Project Snapshot

_Last updated: 2026-06-25_

A backend for a multi-tenant hotel-booking website (many hotels). Reactive Spring
Boot service backed by Postgres (Supabase) with image upload/processing to Supabase
Storage.

---

## Tech stack

| Concern | Choice |
|---|---|
| Language / build | Java 21, Gradle, Spring Boot **4.1.0** |
| Web | Spring WebFlux (reactive), **functional routing** (`RouterFunction` + handlers, not `@RestController`) |
| DB access | Spring Data **R2DBC** (reactive) + `r2dbc-postgresql` |
| Migrations | **Liquibase** over **JDBC** (`spring-jdbc` + `postgresql` driver) |
| Object storage | **Supabase Storage** (REST API via `WebClient`) |
| Image processing | Thumbnailator (compress/thumbnail), TwelveMonkeys ImageIO (HEIC/HEIF decode) |
| Mapping / misc | ModelMapper (entity ↔ domain), Gson, Lombok |
| SSL | netty-tcnative-boringssl-static (required for Supabase R2DBC TLS) |

> Note: R2DBC is reactive; Liquibase needs a **JDBC** URL — these are configured
> separately (`spring.r2dbc.*` vs `spring.liquibase.*`).

## Architecture

Hexagonal / ports-and-adapters per feature module:

```
<module>/
  domain/                     # core model (e.g. Image, Room, Hotel)
  application/
    port/in/                  # use-case interfaces (driving ports)
    port/out/                 # persistence/integration interfaces (driven ports)
    service/                  # use-case implementations
  adapter/
    in/web/  (handler, router, dto)   # HTTP entry (functional routes)
    out/persistence/ (entity, repository, adapter)  # R2DBC
```

Cross-cutting code lives under `core/` (config, exceptions, routes, storage service,
image util).

## Module status

| Module | State | Notes |
|---|---|---|
| **image** | ✅ Implemented | Hotel-scoped CRUD + **room-scoped upload/list/delete** over `khotel_attachment`. Upload → compress + thumbnail → Supabase → persist row. |
| **Room** | ✅ Implemented | Full CRUD nested under a hotel (`/hotel/{hotelId}/room`). Responses **embed the room's images** (batch-loaded to avoid N+1). Validates the parent hotel exists on create. |
| **hotel** | ✅ Implemented | Full CRUD over `khotel_hotel` (create/list/get/update/delete) via the hexagonal stack. Responses don't yet embed rooms/images. |
| **facility** | ✅ Implemented | Catalog CRUD (`khotel_facility`) + assign/list/unassign facilities to hotels and rooms via junction tables. `appliesTo` (HOTEL/ROOM/BOTH) is enforced on assignment. |

## HTTP API

Base path `/api/v1`, port **9090**. Routes require `Accept: application/json`.
See `doc/openapi.yaml` for the image module spec (hotel endpoints not yet in the spec file).

**Hotel** (JSON in/out):

| Method | Path | Purpose |
|---|---|---|
| POST | `/hotel` | Create a hotel (201) |
| GET | `/hotel` | List all hotels |
| GET | `/hotel/{hotelId}` | Get one hotel (404 if missing) |
| PUT | `/hotel/{hotelId}` | Full update (404 if missing) |
| DELETE | `/hotel/{hotelId}` | Delete (204; removes rooms + all images incl. storage) |

**Room** (nested under hotel; responses embed `images`):

| Method | Path | Purpose |
|---|---|---|
| POST | `/hotel/{hotelId}/room` | Create a room (201; 404 if hotel missing) |
| GET | `/hotel/{hotelId}/room` | List the hotel's rooms (images embedded) |
| GET | `/hotel/{hotelId}/room/{roomId}` | Get one room (404 if missing) |
| PUT | `/hotel/{hotelId}/room/{roomId}` | Full update (404 if missing) |
| DELETE | `/hotel/{hotelId}/room/{roomId}` | Delete (204; removes the room's images incl. storage) |

**Facility** (catalog + assignment):

| Method | Path | Purpose |
|---|---|---|
| POST / GET | `/facility` | Create / list catalog facilities |
| GET / PUT / DELETE | `/facility/{facilityId}` | Get / update / delete a facility |
| POST / GET | `/hotel/{hotelId}/facility` | Assign / list a hotel's facilities |
| DELETE | `/hotel/{hotelId}/facility/{facilityId}` | Unassign from hotel |
| POST / GET | `/hotel/{hotelId}/room/{roomId}/facility` | Assign / list a room's facilities |
| DELETE | `/hotel/{hotelId}/room/{roomId}/facility/{facilityId}` | Unassign from room |

**Room images** (nested under room; set `room_id`):

| Method | Path | Purpose |
|---|---|---|
| POST | `/hotel/{hotelId}/room/{roomId}/image` | Upload 1–5 images for a room (multipart `files`) |
| GET | `/hotel/{hotelId}/room/{roomId}/image` | List a room's images |
| DELETE | `/hotel/{hotelId}/room/{roomId}/image/{imageId}` | Delete one room image (row + storage) |
| DELETE | `/hotel/{hotelId}/room/{roomId}/image` | Delete all of a room's images (rows + storage) |

**Image** (hotel-scoped):

| Method | Path | Purpose |
|---|---|---|
| POST | `/image/save/{hotelId}` | Upload 1–5 images (multipart key `files`) |
| GET | `/image/{hotelId}` | List a hotel's images (ordered) |
| GET | `/image/{hotelId}/{imageId}` | Get one image |
| PUT | `/image/{hotelId}/{imageId}` | Replace an image (multipart key `file`) |
| DELETE | `/image/{hotelId}/{imageId}` | Delete one image |
| DELETE | `/image/{hotelId}` | Delete all images for a hotel |

Errors are returned as `{status, error, message, timestamp}` via `GlobalExceptionHandler`
(`ValidationException` → 400, `ImageNotFoundException` → 404, else 500).

## Database

Liquibase master: `src/main/resources/db/changelog/db.changelog-master.yaml`.
Changesets run in order (parents before children for FK validity):
**create-hotel-table → create-room-table → create-image-table → sample-hotel-data → sample-attachment-data**.

**`khotel_hotel`** — the tenant root. `id` is the `hotelId` used everywhere (VARCHAR, accepts
legacy ids like `1111`/`HTL-001` and new UUIDs). Holds name, description, check-in/out times,
contact, address, map url, optimistic `version`, audit. No `imageUrl` column — hotel images
live in `khotel_attachment` (`room_id IS NULL`).

**`khotel_room`** — hard delete (no soft-delete flag), `NUMERIC(12,2)` money, optimistic
`version`, `hotel_id NOT NULL` → FK `khotel_hotel(id) ON DELETE CASCADE`. Also has
`is_air_conditioned` and `prerequisites`.

**`khotel_attachment`** — images for both hotels and rooms:
- `hotel_id NOT NULL` (tenant scope) → FK `khotel_hotel(id) ON DELETE CASCADE`; `room_id` nullable → **NULL = hotel-level image, set = room-level image**.
- FK `room_id → khotel_room(id) ON DELETE CASCADE` (deleting a room removes its image rows; storage objects cleaned up in the service layer).
- Partial unique indexes enforce **one primary image per hotel gallery** and **one per room gallery**.
- Ordered-gallery indexes for both scopes.

**Image ↔ room relationship:** one-to-many via `room_id` on the attachment table (no
image URLs stored on the room row). `Room.images` is a `List<Image>`. This reuses the
single image pipeline for both hotel and room galleries.

**`khotel_facility`** — global, curated facility catalog (`name` unique, `applies_to` =
HOTEL/ROOM/BOTH, `is_active`, version, audit). Shared across all tenants.

**`khotel_hotel_facility` / `khotel_room_facility`** — many-to-many junctions linking the
catalog to hotels/rooms. Surrogate `id` PK (R2DBC needs a single-column id) + UNIQUE
(owner_id, facility_id). Per-offering qualifiers on the link: `is_complimentary`,
`additional_charge`, `notes`. FKs to owner + facility, both `ON DELETE CASCADE` (deleting
a hotel/room/facility removes its links — junctions are DB-only, no storage to clean).

## Configuration

- Active profile: **dev** (`application.properties`). Profile config in `application-dev.properties`.
- Secrets come from a **`.env` at the repo root** (`/home/celloscope/personal-projects/khotel-management/.env`), imported via `spring.config.import`. **Not** `kaptaitourist/.env`. `.env` is gitignored.
- Keys (dotted form, mapped via relaxed binding):
  `SPRING.R2DBC.*`, `SPRING.LIQUIBASE.*`, `SUPABASE.URL`, `SUPABASE.ANON.KEY`, `SUPABASE.STORAGE.BUCKET`.
- DB: Supabase Postgres (pooler host). Storage bucket: **`kaptai`**.
- Multipart limits: 4 MB/file, 20 MB/request. Images also hard-capped at 4 MB and validated to JPEG/PNG/WEBP/HEIC.

## Image pipeline (how upload works)

1. `ImageHandler.saveImage` reads multipart `files` + `createdBy` + `isThumbnail`.
2. `ImageService` validates (max 5/upload), assigns `display_order` by upload order.
3. `ImageUtil` validates type/size, converts HEIC→JPEG, compresses (~70% scale / 0.6 quality; PNG 50%), optionally builds a 400×300 thumbnail. Filenames are sanitized; extension preserved (no double extension).
4. `SupabaseStorageService` PUTs bytes to `…/storage/v1/object/{bucket}/{file}` with `Bearer` auth, returns the public URL.
5. Row persisted to `khotel_attachment` via `ImageAdapter` (ModelMapper entity↔domain).

## Known issues / gotchas

- **Supabase `kaptai` bucket is currently PRIVATE.** The app builds `/object/public/...`
  URLs, which only resolve for public buckets — images won't display until the bucket
  is made public (or the app switches to signed URLs).
- **Liquibase checksums:** the `create-image-table` changeset has already been applied;
  editing its SQL requires resetting the dev schema (drop `khotel_attachment`,
  `khotel_room`, `databasechangelog*`) so changesets re-run. `CREATE TABLE IF NOT EXISTS`
  will not alter an existing table.
- **Hotels now have a table + FKs:** `khotel_hotel` exists; `khotel_room.hotel_id` and
  `khotel_attachment.hotel_id` are real FKs with `ON DELETE CASCADE`. Any seeded/used
  `hotelId` must exist in `khotel_hotel` first (sample data seeds `1111`/`2222`).
- Storage deletes are best-effort (not transactional with the DB) — orphaned storage
  objects are possible on partial failures.

## Next steps (not yet built)

1. **Embed rooms/images/facilities in hotel responses** — hotel CRUD returns hotel columns only; room responses embed images but not facilities yet.
2. **Room-image update (replace) endpoint** — currently you delete + re-upload; a `PUT` replace like the hotel-image one could be added.
3. **Facility list N+1** — `findHotelFacilities`/`findRoomFacilities` do one catalog lookup per link; fine for small sets, but a JOIN/batch could replace it if facility counts grow.
4. Make the `kaptai` bucket public (or implement signed URLs).

## Working agreements

- **Do not run the project** — the developer runs it manually.
- **Do not git commit** — the developer manages commits.
- Verify changes with `./gradlew compileJava` only.
