CREATE TABLE IF NOT EXISTS khotel_attachment
(
    id              VARCHAR(36)                 NOT NULL,
    hotel_id        VARCHAR(36)                 NOT NULL,
    file_url        VARCHAR(1000)               NOT NULL,
    file_name       VARCHAR(255)                NOT NULL,
    file_size_bytes BIGINT                      NOT NULL,
    mime_type       VARCHAR(127)                NOT NULL,
    is_primary      BOOLEAN                     NOT NULL DEFAULT FALSE,
    is_thumbnail    BOOLEAN                     NOT NULL DEFAULT FALSE,
    thumbnail_url   VARCHAR(1000),
    display_order   INTEGER                     NOT NULL DEFAULT 0,
    version         BIGINT                      NOT NULL DEFAULT 0,
    created_by      VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_attachment PRIMARY KEY (id)
    );

-- Enforces one primary image per hotel at the database level
CREATE UNIQUE INDEX IF NOT EXISTS idx_khotel_attachment_one_primary
    ON khotel_attachment (hotel_id)
    WHERE is_primary = TRUE;

-- Ordered gallery fetch: WHERE hotel_id = ? ORDER BY display_order
CREATE INDEX IF NOT EXISTS idx_khotel_attachment_hotel_order
    ON khotel_attachment (hotel_id, display_order);