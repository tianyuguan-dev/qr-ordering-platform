-- Hibernate binds String to VARCHAR; PostgreSQL jsonb column requires cast.
-- Store payload as TEXT (still valid JSON) so JPA works without custom types.
ALTER TABLE outbox_events ALTER COLUMN payload TYPE TEXT USING payload::text;
