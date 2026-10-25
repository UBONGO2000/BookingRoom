-- P1-7 : index de recherche et de pagination.
-- En dev, Hibernate (ddl-auto=update) cree le schema.
-- Ce script sera joue automatiquement quand Flyway sera branche (P2)
-- et sert de reference pour une application manuelle en production.

CREATE INDEX IF NOT EXISTS idx_booking_room_time ON booking (room_id, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_booking_user ON booking (user_id);
CREATE INDEX IF NOT EXISTS idx_room_name ON room (lower(name));
