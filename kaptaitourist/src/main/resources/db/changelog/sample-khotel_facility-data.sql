-- khotel_facility catalog seed (common facilities).
INSERT INTO khotel_facility (id, name, category, icon, description, applies_to, is_active, version, created_by, created_at)
VALUES
    ('11111111-aaaa-0000-0000-000000000001', 'Free WiFi',        'CONNECTIVITY',   'wifi',       'Wireless internet access',          'BOTH',  true, 0, 'seed', CURRENT_TIMESTAMP),
    ('11111111-aaaa-0000-0000-000000000002', 'Free Breakfast',   'DINING',         'breakfast',  'Complimentary breakfast',           'BOTH',  true, 0, 'seed', CURRENT_TIMESTAMP),
    ('11111111-aaaa-0000-0000-000000000003', 'Swimming Pool',    'RECREATION',     'pool',       'Outdoor swimming pool',             'HOTEL', true, 0, 'seed', CURRENT_TIMESTAMP),
    ('11111111-aaaa-0000-0000-000000000004', 'Kayaking',         'RECREATION',     'kayak',      'Lake kayaking',                     'HOTEL', true, 0, 'seed', CURRENT_TIMESTAMP),
    ('11111111-aaaa-0000-0000-000000000005', 'On-site Dining',   'DINING',         'restaurant', 'Restaurant on the premises',        'HOTEL', true, 0, 'seed', CURRENT_TIMESTAMP),
    ('11111111-aaaa-0000-0000-000000000006', 'Business Spaces',  'BUSINESS',       'business',   'Meeting and work spaces',           'HOTEL', true, 0, 'seed', CURRENT_TIMESTAMP),
    ('11111111-aaaa-0000-0000-000000000007', 'Guest Services',   'GUEST_SERVICES', 'concierge',  '24/7 guest services',               'HOTEL', true, 0, 'seed', CURRENT_TIMESTAMP),
    ('11111111-aaaa-0000-0000-000000000008', 'Air Conditioning', 'COMFORT',        'ac',         'In-room air conditioning',          'ROOM',  true, 0, 'seed', CURRENT_TIMESTAMP);
