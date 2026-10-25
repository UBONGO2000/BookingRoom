-- P1-6 : annulation logique d'une reservation.
-- En dev, Hibernate (ddl-auto=update) cree deja ces colonnes.
-- Ce script sera joue automatiquement quand Flyway sera branche (P2),
-- et sert de reference pour une application manuelle en production.

ALTER TABLE booking ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';
ALTER TABLE booking ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;
