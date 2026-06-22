CREATE TABLE IF NOT EXISTS khotel_attachment
(
    id              VARCHAR(36)                 NOT NULL,
    hotel_id        VARCHAR(36)                 NOT NULL,
    file_url        TEXT                        NOT NULL,
    file_name       VARCHAR(255)                NOT NULL,
    file_size_bytes INTEGER                     NOT NULL,
    mime_type       VARCHAR(127)                NOT NULL,
    is_primary      BOOLEAN                     NOT NULL DEFAULT FALSE,
    display_order   INTEGER                     NOT NULL DEFAULT 0,
    version         BIGINT                      NOT NULL DEFAULT 0,
    created_by      VARCHAR(255)                NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    is_deleted      BOOLEAN                     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_khotel_attachment PRIMARY KEY (id)
);

-- Active images per hotel (primary read pattern)
CREATE INDEX IF NOT EXISTS idx_khotel_attachment_hotel_active
    ON khotel_attachment (hotel_id)
    WHERE is_deleted = FALSE;

-- Enforces one primary image per hotel at the database level
CREATE UNIQUE INDEX IF NOT EXISTS idx_khotel_attachment_one_primary
    ON khotel_attachment (hotel_id)
    WHERE is_primary = TRUE AND is_deleted = FALSE;

-- Ordered gallery fetch: WHERE hotel_id = ? AND is_deleted = FALSE ORDER BY display_order
CREATE INDEX IF NOT EXISTS idx_khotel_attachment_hotel_order
    ON khotel_attachment (hotel_id, display_order)
    WHERE is_deleted = FALSE;