-- Supabase Auth owns password verification now; users created via the Supabase mirror
-- flow have no local password. Relax the NOT NULL so those rows can be stored. The old
-- local-auth module still writes a bcrypt hash; dropping the column entirely is deferred.
ALTER TABLE khotel_user ALTER COLUMN password_hash DROP NOT NULL;
