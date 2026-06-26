CREATE TABLE IF NOT EXISTS khotel_room
(
    id                 VARCHAR(36)                 NOT NULL,
    hotel_id           VARCHAR(36)                 NOT NULL,
    room_name          VARCHAR(255)                NOT NULL,
    capacity           INTEGER                     NOT NULL DEFAULT 1,   -- guests per unit
    total_units        INTEGER                     NOT NULL DEFAULT 1,   -- physical rooms of this type (e.g. 7 Premium rooms)
    room_type          VARCHAR(64),
    is_air_conditioned BOOLEAN                     NOT NULL DEFAULT FALSE,
    description        TEXT,
    prerequisites      TEXT,
    price_per_night    NUMERIC(12, 2)              NOT NULL DEFAULT 0,
    discount           NUMERIC(12, 2)              NOT NULL DEFAULT 0,
    is_available       BOOLEAN                     NOT NULL DEFAULT TRUE,
    version            BIGINT                      NOT NULL DEFAULT 0,
    created_by         VARCHAR(255),
    created_at         TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(255),
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_room PRIMARY KEY (id),
    CONSTRAINT fk_khotel_room_hotel FOREIGN KEY (hotel_id)
        REFERENCES khotel_hotel (id) ON DELETE CASCADE
    );

-- List a hotel's rooms
CREATE INDEX IF NOT EXISTS idx_khotel_room_hotel
    ON khotel_room (hotel_id);
