-- khotel_hotel sample data
-- Seeds the two hotels referenced by the attachment sample data (1111, 2222)
-- so their hotel_id foreign keys resolve.

INSERT INTO khotel_hotel
    (id, name, description, check_in_time, check_out_time, mobile, email, website,
     address, google_map_url, version, created_by, created_at, updated_by, updated_at)
VALUES
    ('1111', 'Lake View Resort', 'Lakeside resort with panoramic views of Kaptai Lake.',
     '14:00', '12:00', '+8801700000000', 'info@lakeview.example', 'https://lakeview.example',
     'Kaptai, Rangamati', 'https://maps.google.com/?q=Kaptai+Lake+View', 0, 'seed', CURRENT_TIMESTAMP, NULL, NULL),

    ('2222', 'Hilltop Inn', 'Cozy hilltop inn overlooking the valley.',
     '13:00', '11:00', '+8801800000000', 'info@hilltop.example', 'https://hilltop.example',
     'Hilltop Road, Rangamati', 'https://maps.google.com/?q=Hilltop+Inn', 0, 'seed', CURRENT_TIMESTAMP, NULL, NULL);
