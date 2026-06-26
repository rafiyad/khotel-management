-- Roles (reference data, seeded below). Coarse RBAC: USER | HOTEL_OWNER | ADMIN.
CREATE TABLE IF NOT EXISTS khotel_role
(
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_khotel_role PRIMARY KEY (id),
    CONSTRAINT uq_khotel_role_name UNIQUE (name)
    );

INSERT INTO khotel_role (id, name, description)
VALUES ('11111111-0000-0000-0000-000000000001', 'USER', 'Default role; can browse and book'),
       ('11111111-0000-0000-0000-000000000002', 'HOTEL_OWNER', 'Manages the hotels they own'),
       ('11111111-0000-0000-0000-000000000003', 'ADMIN', 'Full control')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS khotel_user
(
    id            VARCHAR(36)                 NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    email         VARCHAR(255)                NOT NULL,
    mobile        VARCHAR(32)                 NOT NULL,
    gender        VARCHAR(20),
    password_hash VARCHAR(255)                NOT NULL,
    is_active     BOOLEAN                     NOT NULL DEFAULT TRUE,
    version       BIGINT                      NOT NULL DEFAULT 0,
    created_by    VARCHAR(255),
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(255),
    updated_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_user PRIMARY KEY (id),
    CONSTRAINT uq_khotel_user_email UNIQUE (email),
    CONSTRAINT uq_khotel_user_mobile UNIQUE (mobile),
    CONSTRAINT chk_khotel_user_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'))
    );

-- User ↔ role (a user may hold multiple roles). Surrogate id (R2DBC needs one).
CREATE TABLE IF NOT EXISTS khotel_user_role
(
    id         VARCHAR(36)                 NOT NULL,
    user_id    VARCHAR(36)                 NOT NULL,
    role_id    VARCHAR(36)                 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_khotel_user_role PRIMARY KEY (id),
    CONSTRAINT uq_khotel_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_khotel_user_role_user FOREIGN KEY (user_id)
        REFERENCES khotel_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_khotel_user_role_role FOREIGN KEY (role_id)
        REFERENCES khotel_role (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_khotel_user_role_user ON khotel_user_role (user_id);
