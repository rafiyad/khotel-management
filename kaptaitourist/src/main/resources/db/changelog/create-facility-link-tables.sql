-- Many-to-many: a hotel offers many facilities; a facility is offered by many hotels.
-- Surrogate `id` PK (R2DBC needs a single-column id); UNIQUE(hotel_id, facility_id)
-- prevents duplicate assignments. Per-offering qualifiers live on the link.
CREATE TABLE IF NOT EXISTS khotel_hotel_facility
(
    id                VARCHAR(36)                 NOT NULL,
    hotel_id          VARCHAR(36)                 NOT NULL,
    facility_id       VARCHAR(36)                 NOT NULL,
    is_complimentary  BOOLEAN                     NOT NULL DEFAULT TRUE,
    is_available      BOOLEAN                     NOT NULL DEFAULT TRUE,   -- owner can mark temporarily unavailable
    additional_charge NUMERIC(12, 2),
    notes             VARCHAR(255),
    created_by        VARCHAR(255),
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_khotel_hotel_facility PRIMARY KEY (id),
    CONSTRAINT uq_khotel_hotel_facility UNIQUE (hotel_id, facility_id),
    CONSTRAINT fk_khotel_hotel_facility_hotel FOREIGN KEY (hotel_id)
        REFERENCES khotel_hotel (id) ON DELETE CASCADE,
    CONSTRAINT fk_khotel_hotel_facility_facility FOREIGN KEY (facility_id)
        REFERENCES khotel_facility (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_khotel_hotel_facility_hotel ON khotel_hotel_facility (hotel_id);
CREATE INDEX IF NOT EXISTS idx_khotel_hotel_facility_facility ON khotel_hotel_facility (facility_id);

CREATE TABLE IF NOT EXISTS khotel_room_facility
(
    id                VARCHAR(36)                 NOT NULL,
    room_id           VARCHAR(36)                 NOT NULL,
    facility_id       VARCHAR(36)                 NOT NULL,
    is_complimentary  BOOLEAN                     NOT NULL DEFAULT TRUE,
    is_available      BOOLEAN                     NOT NULL DEFAULT TRUE,   -- owner can mark temporarily unavailable
    additional_charge NUMERIC(12, 2),
    notes             VARCHAR(255),
    created_by        VARCHAR(255),
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_khotel_room_facility PRIMARY KEY (id),
    CONSTRAINT uq_khotel_room_facility UNIQUE (room_id, facility_id),
    CONSTRAINT fk_khotel_room_facility_room FOREIGN KEY (room_id)
        REFERENCES khotel_room (id) ON DELETE CASCADE,
    CONSTRAINT fk_khotel_room_facility_facility FOREIGN KEY (facility_id)
        REFERENCES khotel_facility (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_khotel_room_facility_room ON khotel_room_facility (room_id);
CREATE INDEX IF NOT EXISTS idx_khotel_room_facility_facility ON khotel_room_facility (facility_id);
