-- The four Supabase auth endpoints are PUBLIC (no token). RBAC is data-driven, so they
-- must exist as permission rows with permission_name = 'ALL' (see create-rbac-tables.sql),
-- otherwise RbacFilter denies them with 401. Guarded with NOT EXISTS so it is safe to
-- re-run against a database that already holds these rows.
INSERT INTO permission (id, permission_name, url, method, service_name)
SELECT CAST(gen_random_uuid() AS VARCHAR), v.permission_name, v.url, v.method, v.service_name
FROM (VALUES
    ('ALL', '/api/v1/supabase/auth/register',       'POST', 'supabaseauth'),
    ('ALL', '/api/v1/supabase/auth/login',          'POST', 'supabaseauth'),
    ('ALL', '/api/v1/supabase/auth/forgotpassword', 'POST', 'supabaseauth'),
    ('ALL', '/api/v1/supabase/auth/resetpassword',  'POST', 'supabaseauth')
) AS v(permission_name, url, method, service_name)
WHERE NOT EXISTS (
    SELECT 1 FROM permission p
    WHERE p.url = v.url AND p.method = v.method AND p.is_deleted = FALSE
);
