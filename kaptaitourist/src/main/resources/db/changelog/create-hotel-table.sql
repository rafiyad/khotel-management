CREATE TABLE IF NOT EXISTS khotel_hotel
(
    id              VARCHAR(36)                 NOT NULL,
    name            VARCHAR(255)                NOT NULL,
    description     TEXT,
    check_in_time   TIME WITHOUT TIME ZONE,
    check_out_time  TIME WITHOUT TIME ZONE,
    mobile          VARCHAR(32),
    email           VARCHAR(255),
    website         VARCHAR(512),
    address         TEXT,
    google_map_url  VARCHAR(1000),
    version         BIGINT                      NOT NULL DEFAULT 0,
    created_by      VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_hotel PRIMARY KEY (id)
    );
