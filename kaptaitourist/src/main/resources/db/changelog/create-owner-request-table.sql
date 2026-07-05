-- Owner enlistment requests. A visitor self-registers as a USER and files a PENDING request to
-- become a HOTEL_OWNER; an ADMIN approves (grants HOTEL_OWNER) or rejects. Decided requests are
-- terminal (cannot be re-decided).
CREATE TABLE IF NOT EXISTS khotel_owner_request
(
    id          VARCHAR(36)                 NOT NULL,
    user_id     VARCHAR(36)                 NOT NULL,
    hotel_name  VARCHAR(255)                NOT NULL,
    message     TEXT,
    status      VARCHAR(20)                 NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED
    decided_by  VARCHAR(36),                                            -- admin userId who approved/rejected
    decided_at  TIMESTAMP WITHOUT TIME ZONE,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_khotel_owner_request PRIMARY KEY (id),
    CONSTRAINT fk_khotel_owner_request_user FOREIGN KEY (user_id)
        REFERENCES khotel_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_khotel_owner_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
    );

CREATE INDEX IF NOT EXISTS idx_khotel_owner_request_status ON khotel_owner_request (status);
CREATE INDEX IF NOT EXISTS idx_khotel_owner_request_user ON khotel_owner_request (user_id);
