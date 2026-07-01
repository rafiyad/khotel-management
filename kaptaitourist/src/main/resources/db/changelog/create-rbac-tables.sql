-- ╔═══════════════════════════════════════════════════════════════════════════════╗
-- ║ Data-driven RBAC: role ── role_permission ── permission                          ║
-- ╚═══════════════════════════════════════════════════════════════════════════════╝
-- Authorization is decided at request time (RbacFilter → PermissionService) by matching
-- the request's HTTP method + URL template against permission rows linked to the caller's
-- roles. A permission row with permission_name = 'ALL' marks a PUBLIC endpoint reachable
-- without authentication. Hard delete elsewhere in this schema, but these tables carry an
-- is_deleted flag because the queries filter on it.
--
-- NOTE: this `role` table is the RBAC anchor for permission links. User↔role MEMBERSHIP
-- still lives in khotel_role / khotel_user_role (and the JWT). The role NAMES here must
-- match those (USER / HOTEL_OWNER / ADMIN) for the name-based join to work.

-- ───────────────────────────────── role ─────────────────────────────────
CREATE TABLE IF NOT EXISTS role
(
    id          VARCHAR(36)                 NOT NULL,
    name        VARCHAR(50)                 NOT NULL,
    description VARCHAR(255),
    is_deleted  BOOLEAN                     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_role PRIMARY KEY (id)
    );
CREATE UNIQUE INDEX IF NOT EXISTS uq_role_name ON role (name) WHERE is_deleted = FALSE;

-- ───────────────────────────────── permission ───────────────────────────
CREATE TABLE IF NOT EXISTS permission
(
    id              VARCHAR(36)                 NOT NULL,
    permission_name VARCHAR(128)                NOT NULL,   -- logical name, e.g. HOTEL.UPDATE; 'ALL' = public
    url             VARCHAR(256)                NOT NULL,   -- path template, e.g. /api/v1/hotel/{hotelId}
    method          VARCHAR(16)                 NOT NULL,   -- GET | POST | PUT | DELETE
    service_name    VARCHAR(64),                            -- owning module
    top_menu_id     VARCHAR(256),                           -- optional: drives frontend top menu
    left_menu_id    VARCHAR(256),                           -- optional: drives frontend left menu
    is_deleted      BOOLEAN                     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_permission PRIMARY KEY (id)
    );
CREATE UNIQUE INDEX IF NOT EXISTS uq_permission_url_method ON permission (url, method) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_permission_method ON permission (method) WHERE is_deleted = FALSE;

-- ───────────────────────────────── role_permission ──────────────────────
CREATE TABLE IF NOT EXISTS role_permission
(
    id            VARCHAR(36)                 NOT NULL,
    role_id       VARCHAR(36)                 NOT NULL,
    permission_id VARCHAR(36)                 NOT NULL,
    is_deleted    BOOLEAN                     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_permission PRIMARY KEY (id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id)
        REFERENCES role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id)
        REFERENCES permission (id) ON DELETE CASCADE
    );
CREATE UNIQUE INDEX IF NOT EXISTS uq_role_permission ON role_permission (role_id, permission_id) WHERE is_deleted = FALSE;

-- ═════════════════════════════════ Seed: roles ══════════════════════════════════
-- IDs are generated as UUID v4 (gen_random_uuid()); nothing references them by literal
-- value — role_permission links are resolved by role NAME / permission_name below.
INSERT INTO role (id, name, description)
SELECT CAST(gen_random_uuid() AS VARCHAR), v.name, v.description
FROM (VALUES
    ('USER',        'Default role; can browse and book'),
    ('HOTEL_OWNER', 'Manages hotels and their inventory'),
    ('ADMIN',       'Full control')
) AS v(name, description);

-- ═════════════════════════════ Seed: permissions (42) ═══════════════════════════
-- Public endpoints carry permission_name = 'ALL' (no role link needed). IDs = UUID v4.
INSERT INTO permission (id, permission_name, url, method, service_name)
SELECT CAST(gen_random_uuid() AS VARCHAR), v.permission_name, v.url, v.method, v.service_name
FROM (VALUES
    -- ── Public (no auth) ──
    ('ALL', '/api/v1/auth/register',                              'POST',   'user'),
    ('ALL', '/api/v1/auth/login',                                 'POST',   'user'),
    ('ALL', '/api/v1/hotel',                                      'GET',    'hotel'),
    ('ALL', '/api/v1/hotel/{hotelId}',                            'GET',    'hotel'),
    ('ALL', '/api/v1/hotel/{hotelId}/room',                       'GET',    'room'),
    ('ALL', '/api/v1/hotel/{hotelId}/room/{roomId}',              'GET',    'room'),
    ('ALL', '/api/v1/hotel/{hotelId}/room/{roomId}/availability', 'GET',    'booking'),

    -- ── Auth / user (protected) ──
    ('AUTH.ME',              '/api/v1/auth/me',                   'GET',    'user'),
    ('AUTH.PROFILE',         '/api/v1/auth/profile',              'GET',    'user'),
    ('USER.LIST',            '/api/v1/user',                      'GET',    'user'),
    ('USER.PROMOTE',         '/api/v1/user/{userId}/promote',     'POST',   'user'),

    -- ── Hotel ──
    ('HOTEL.CREATE',         '/api/v1/hotel',                     'POST',   'hotel'),
    ('HOTEL.UPDATE',         '/api/v1/hotel/{hotelId}',           'PUT',    'hotel'),
    ('HOTEL.DELETE',         '/api/v1/hotel/{hotelId}',           'DELETE', 'hotel'),

    -- ── Room ──
    ('ROOM.CREATE',          '/api/v1/hotel/{hotelId}/room',                  'POST',   'room'),
    ('ROOM.UPDATE',          '/api/v1/hotel/{hotelId}/room/{roomId}',         'PUT',    'room'),
    ('ROOM.DELETE',          '/api/v1/hotel/{hotelId}/room/{roomId}',         'DELETE', 'room'),

    -- ── Facility catalog ──
    ('FACILITY.CREATE',      '/api/v1/facility',                  'POST',   'facility'),
    ('FACILITY.LIST',        '/api/v1/facility',                  'GET',    'facility'),
    ('FACILITY.GET',         '/api/v1/facility/{facilityId}',     'GET',    'facility'),
    ('FACILITY.UPDATE',      '/api/v1/facility/{facilityId}',     'PUT',    'facility'),
    ('FACILITY.DELETE',      '/api/v1/facility/{facilityId}',     'DELETE', 'facility'),

    -- ── Facility ↔ hotel ──
    ('HOTEL_FACILITY.ASSIGN','/api/v1/hotel/{hotelId}/facility',                  'POST',   'facility'),
    ('HOTEL_FACILITY.LIST',  '/api/v1/hotel/{hotelId}/facility',                  'GET',    'facility'),
    ('HOTEL_FACILITY.REMOVE','/api/v1/hotel/{hotelId}/facility/{facilityId}',     'DELETE', 'facility'),

    -- ── Facility ↔ room ──
    ('ROOM_FACILITY.ASSIGN', '/api/v1/hotel/{hotelId}/room/{roomId}/facility',                'POST',   'facility'),
    ('ROOM_FACILITY.LIST',   '/api/v1/hotel/{hotelId}/room/{roomId}/facility',                'GET',    'facility'),
    ('ROOM_FACILITY.REMOVE', '/api/v1/hotel/{hotelId}/room/{roomId}/facility/{facilityId}',   'DELETE', 'facility'),

    -- ── Booking ──
    ('BOOKING.CREATE',       '/api/v1/hotel/{hotelId}/room/{roomId}/booking',     'POST',   'booking'),
    ('BOOKING.LIST',         '/api/v1/hotel/{hotelId}/booking',                   'GET',    'booking'),
    ('BOOKING.GET',          '/api/v1/hotel/{hotelId}/booking/{bookingId}',       'GET',    'booking'),
    ('BOOKING.CANCEL',       '/api/v1/hotel/{hotelId}/booking/{bookingId}/cancel','POST',   'booking'),

    -- ── Hotel-level images (flat /image route) ──
    ('IMAGE.SAVE',           '/api/v1/image/save/{hotelId}',          'POST',   'image'),
    ('IMAGE.LIST',           '/api/v1/image/{hotelId}',               'GET',    'image'),
    ('IMAGE.GET',            '/api/v1/image/{hotelId}/{imageId}',     'GET',    'image'),
    ('IMAGE.UPDATE',         '/api/v1/image/{hotelId}/{imageId}',     'PUT',    'image'),
    ('IMAGE.DELETE',         '/api/v1/image/{hotelId}/{imageId}',     'DELETE', 'image'),
    ('IMAGE.DELETE_ALL',     '/api/v1/image/{hotelId}',               'DELETE', 'image'),

    -- ── Room-level images (nested route) ──
    ('ROOM_IMAGE.SAVE',      '/api/v1/hotel/{hotelId}/room/{roomId}/image',              'POST',   'image'),
    ('ROOM_IMAGE.LIST',      '/api/v1/hotel/{hotelId}/room/{roomId}/image',              'GET',    'image'),
    ('ROOM_IMAGE.DELETE',    '/api/v1/hotel/{hotelId}/room/{roomId}/image/{imageId}',    'DELETE', 'image'),
    ('ROOM_IMAGE.DELETE_ALL','/api/v1/hotel/{hotelId}/room/{roomId}/image',              'DELETE', 'image')
) AS v(permission_name, url, method, service_name);

-- ═════════════════════════ Seed: role → permission links ════════════════════════
-- ADMIN: every protected endpoint.
INSERT INTO role_permission (id, role_id, permission_id)
SELECT CAST(gen_random_uuid() AS VARCHAR), r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.name = 'ADMIN' AND p.permission_name <> 'ALL';

-- HOTEL_OWNER: everything except user administration and facility-catalog writes.
INSERT INTO role_permission (id, role_id, permission_id)
SELECT CAST(gen_random_uuid() AS VARCHAR), r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.name = 'HOTEL_OWNER'
  AND p.permission_name NOT IN ('ALL', 'USER.LIST', 'USER.PROMOTE',
                                'FACILITY.CREATE', 'FACILITY.UPDATE', 'FACILITY.DELETE');

-- USER: own identity views + booking a room (browsing is public).
INSERT INTO role_permission (id, role_id, permission_id)
SELECT CAST(gen_random_uuid() AS VARCHAR), r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.name = 'USER'
  AND p.permission_name IN ('AUTH.ME', 'AUTH.PROFILE', 'BOOKING.CREATE');
