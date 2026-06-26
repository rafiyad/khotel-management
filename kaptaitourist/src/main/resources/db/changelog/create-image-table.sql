CREATE TABLE IF NOT EXISTS khotel_attachment
(
    id              VARCHAR(36)                 NOT NULL,
    hotel_id        VARCHAR(36)                 NOT NULL,
    room_id         VARCHAR(36),                          -- NULL = hotel-level image; set = room-level image
    file_url        VARCHAR(1000)               NOT NULL,
    file_name       VARCHAR(255)                NOT NULL,
    file_size_bytes BIGINT                      NOT NULL,
    mime_type       VARCHAR(127)                NOT NULL,
    is_primary      BOOLEAN                     NOT NULL DEFAULT FALSE,
    thumbnail_url   VARCHAR(1000),
    display_order   INTEGER                     NOT NULL DEFAULT 0,
    version         BIGINT                      NOT NULL DEFAULT 0,
    created_by      VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_attachment PRIMARY KEY (id),
    CONSTRAINT fk_khotel_attachment_hotel FOREIGN KEY (hotel_id)
        REFERENCES khotel_hotel (id) ON DELETE CASCADE,
    CONSTRAINT fk_khotel_attachment_room FOREIGN KEY (room_id)
        REFERENCES khotel_room (id) ON DELETE CASCADE
    );

-- Enforces one primary image per hotel gallery (hotel-level images only)
CREATE UNIQUE INDEX IF NOT EXISTS idx_khotel_attachment_hotel_one_primary
    ON khotel_attachment (hotel_id)
    WHERE is_primary = TRUE AND room_id IS NULL;

-- Enforces one primary image per room gallery
CREATE UNIQUE INDEX IF NOT EXISTS idx_khotel_attachment_room_one_primary
    ON khotel_attachment (room_id)
    WHERE is_primary = TRUE AND room_id IS NOT NULL;

-- Ordered hotel gallery fetch: WHERE hotel_id = ? AND room_id IS NULL ORDER BY display_order
CREATE INDEX IF NOT EXISTS idx_khotel_attachment_hotel_order
    ON khotel_attachment (hotel_id, display_order)
    WHERE room_id IS NULL;

-- Ordered room gallery fetch: WHERE room_id = ? ORDER BY display_order
CREATE INDEX IF NOT EXISTS idx_khotel_attachment_room_order
    ON khotel_attachment (room_id, display_order);
