-- P1-7 / P2-20 : index de recherche (variante H2 utilisee par les tests).
-- H2 ne supporte pas les index sur expression : on indexe la colonne name.
-- La variante PostgreSQL (db/migration/postgresql) utilise lower(name).

CREATE INDEX IF NOT EXISTS idx_booking_room_time ON booking (room_id, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_booking_user ON booking (user_id);
CREATE INDEX IF NOT EXISTS idx_room_name ON room (name);
