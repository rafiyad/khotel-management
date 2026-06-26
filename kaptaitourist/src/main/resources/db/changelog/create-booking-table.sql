-- Reservations against a room TYPE for a date range. Availability is computed from
-- these rows (total_units - overlapping non-cancelled units), never stored as a counter.
CREATE TABLE IF NOT EXISTS khotel_booking
(
    id               VARCHAR(36)                 NOT NULL,
    hotel_id         VARCHAR(36)                 NOT NULL,
    room_id          VARCHAR(36)                 NOT NULL,
    check_in         DATE                        NOT NULL,
    check_out        DATE                        NOT NULL,
    units            INTEGER                     NOT NULL DEFAULT 1,
    guest_name       VARCHAR(255)                NOT NULL,
    guest_phone      VARCHAR(32),
    guest_email      VARCHAR(255),
    number_of_guests INTEGER,
    status           VARCHAR(20)                 NOT NULL DEFAULT 'CONFIRMED',  -- CONFIRMED | CANCELLED
    total_price      NUMERIC(12, 2),
    version          BIGINT                      NOT NULL DEFAULT 0,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(255),
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_booking PRIMARY KEY (id),
    CONSTRAINT fk_khotel_booking_hotel FOREIGN KEY (hotel_id)
        REFERENCES khotel_hotel (id) ON DELETE CASCADE,
    CONSTRAINT fk_khotel_booking_room FOREIGN KEY (room_id)
        REFERENCES khotel_room (id) ON DELETE CASCADE,
    CONSTRAINT chk_khotel_booking_dates CHECK (check_out > check_in),
    CONSTRAINT chk_khotel_booking_units CHECK (units > 0)
    );

-- Availability query: overlapping non-cancelled bookings for a room type.
CREATE INDEX IF NOT EXISTS idx_khotel_booking_room_dates
    ON khotel_booking (room_id, check_in, check_out)
    WHERE status <> 'CANCELLED';

-- List a hotel's bookings.
CREATE INDEX IF NOT EXISTS idx_khotel_booking_hotel
    ON khotel_booking (hotel_id);
