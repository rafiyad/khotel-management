-- Global, curated facility catalog (shared across all hotels/rooms).
CREATE TABLE IF NOT EXISTS khotel_facility
(
    id           VARCHAR(36)                 NOT NULL,
    name         VARCHAR(100)                NOT NULL,
    category     VARCHAR(50),                          -- CONNECTIVITY | DINING | RECREATION | BUSINESS | GUEST_SERVICES | COMFORT ...
    icon         VARCHAR(100),                         -- icon key for the UI
    description  TEXT,
    applies_to   VARCHAR(10)                 NOT NULL DEFAULT 'BOTH',  -- HOTEL | ROOM | BOTH
    is_active    BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_by_id VARCHAR(36),                         -- user id of the creator; NULL = legacy/admin catalog. HOTEL_OWNER may only edit/delete their own.
    version      BIGINT                      NOT NULL DEFAULT 0,
    created_by   VARCHAR(255),                         -- free-text audit label (kept separate from created_by_id)
    created_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(255),
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_facility PRIMARY KEY (id),
    CONSTRAINT uq_khotel_facility_name UNIQUE (name)
    );
