-- khotel_attachment sample data
-- Two hotels, a small gallery each. Exactly one primary image per hotel
-- (enforced by idx_khotel_attachment_one_primary). One soft-deleted row included.

INSERT INTO khotel_attachment
    (id, hotel_id, file_url, file_name, file_size_bytes, mime_type,
     is_primary, display_order, version, created_by, created_at,
     updated_by, updated_at, is_deleted)
VALUES
    -- Hotel 1111 — Lake View Resort
    ('11111111-1111-1111-1111-111111111111', '1111', 'https://cdn.kaptai.example/hotels/1111/lobby.jpg',
     'lobby.jpg', 245678, 'image/jpeg', true, 0, 0, 'seed', CURRENT_TIMESTAMP, NULL, NULL, false),

    ('11111111-1111-1111-1111-111111111112', '1111', 'https://cdn.kaptai.example/hotels/1111/deluxe-room.jpg',
     'deluxe-room.jpg', 312045, 'image/jpeg', false, 1, 0, 'seed', CURRENT_TIMESTAMP, NULL, NULL, false),

    ('11111111-1111-1111-1111-111111111113', '1111', 'https://cdn.kaptai.example/hotels/1111/lake-view.png',
     'lake-view.png', 1048576, 'image/png', false, 2, 0, 'seed', CURRENT_TIMESTAMP, NULL, NULL, false),

    -- Hotel 2222 — Hilltop Inn
    ('22222222-2222-2222-2222-222222222221', '2222', 'https://cdn.kaptai.example/hotels/2222/exterior.jpg',
     'exterior.jpg', 198432, 'image/jpeg', true, 0, 0, 'seed', CURRENT_TIMESTAMP, NULL, NULL, false),

    ('22222222-2222-2222-2222-222222222222', '2222', 'https://cdn.kaptai.example/hotels/2222/restaurant.webp',
     'restaurant.webp', 87654, 'image/webp', false, 1, 0, 'seed', CURRENT_TIMESTAMP, NULL, NULL, false),

    -- Soft-deleted row: excluded by partial indexes and the active read pattern
    ('22222222-2222-2222-2222-222222222223', '2222', 'https://cdn.kaptai.example/hotels/2222/old-pool.jpg',
     'old-pool.jpg', 156000, 'image/jpeg', false, 2, 1, 'seed', CURRENT_TIMESTAMP,
     'seed', CURRENT_TIMESTAMP, true);
