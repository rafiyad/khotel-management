-- Which users own/manage which hotels. Drives "owner can only modify their own hotel".
CREATE TABLE IF NOT EXISTS khotel_hotel_owner
(
    id         VARCHAR(36)                 NOT NULL,
    user_id    VARCHAR(36)                 NOT NULL,
    hotel_id   VARCHAR(36)                 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_khotel_hotel_owner PRIMARY KEY (id),
    CONSTRAINT uq_khotel_hotel_owner UNIQUE (user_id, hotel_id),
    CONSTRAINT fk_khotel_hotel_owner_user FOREIGN KEY (user_id)
        REFERENCES khotel_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_khotel_hotel_owner_hotel FOREIGN KEY (hotel_id)
        REFERENCES khotel_hotel (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_khotel_hotel_owner_user ON khotel_hotel_owner (user_id);
CREATE INDEX IF NOT EXISTS idx_khotel_hotel_owner_hotel ON khotel_hotel_owner (hotel_id);
