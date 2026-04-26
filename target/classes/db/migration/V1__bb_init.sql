-- Flyway baseline migration for the Definition Blackboard Manager substrate.
-- Re-runnable: every DDL uses IF NOT EXISTS / OR REPLACE.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- UUIDv7 generator (time-ordered), pure pl/pgsql.
CREATE OR REPLACE FUNCTION uuid_v7() RETURNS uuid AS $$
DECLARE
  unix_ms BIGINT;
  rand_bytes BYTEA;
  uuid_bytes BYTEA;
BEGIN
  unix_ms := (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT;
  rand_bytes := gen_random_bytes(10);
  uuid_bytes := SET_BYTE(SET_BYTE(SET_BYTE(SET_BYTE(SET_BYTE(SET_BYTE(
                  rand_bytes || rand_bytes,
                  0, ((unix_ms >> 40) & 255)::INT),
                  1, ((unix_ms >> 32) & 255)::INT),
                  2, ((unix_ms >> 24) & 255)::INT),
                  3, ((unix_ms >> 16) & 255)::INT),
                  4, ((unix_ms >> 8)  & 255)::INT),
                  5, ( unix_ms        & 255)::INT);
  -- version 7
  uuid_bytes := SET_BYTE(uuid_bytes, 6, ((GET_BYTE(uuid_bytes, 6) & 15) | 112));
  -- variant 10xx
  uuid_bytes := SET_BYTE(uuid_bytes, 8, ((GET_BYTE(uuid_bytes, 8) & 63) | 128));
  RETURN ENCODE(SUBSTRING(uuid_bytes FROM 1 FOR 16), 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

CREATE TABLE IF NOT EXISTS blackboard (
  id              uuid        NOT NULL DEFAULT uuid_v7() PRIMARY KEY,
  name            text        NOT NULL,
  "timestamp"     timestamptz NOT NULL DEFAULT clock_timestamp(),
  status          text        NOT NULL CHECK (status IN ('OPEN','SEALING','SEALED','ABORTED')),
  seal_timestamp  timestamptz NULL
);

CREATE TABLE IF NOT EXISTS panel (
  id              uuid NOT NULL DEFAULT uuid_v7() PRIMARY KEY,
  blackboard_id   uuid NOT NULL REFERENCES blackboard(id) ON DELETE CASCADE,
  name            text NOT NULL,
  description     text NOT NULL,
  CONSTRAINT panel_unique_in_blackboard UNIQUE (blackboard_id, name)
);
CREATE INDEX IF NOT EXISTS ix_panel_blackboard_id ON panel(blackboard_id);

CREATE TABLE IF NOT EXISTS contribution_slot (
  id            uuid NOT NULL DEFAULT uuid_v7() PRIMARY KEY,
  panel_id      uuid NOT NULL REFERENCES panel(id) ON DELETE CASCADE,
  name          text NOT NULL,
  schema_uri    text NOT NULL,
  CONSTRAINT slot_unique_in_panel UNIQUE (panel_id, name)
);
CREATE INDEX IF NOT EXISTS ix_slot_panel_id ON contribution_slot(panel_id);

CREATE TABLE IF NOT EXISTS contribution (
  id                    uuid        NOT NULL DEFAULT uuid_v7() PRIMARY KEY,
  contribution_slot_id  uuid        NOT NULL REFERENCES contribution_slot(id) ON DELETE CASCADE,
  post                  jsonb       NOT NULL,
  "timestamp"           timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX IF NOT EXISTS ix_contribution_slot_id ON contribution(contribution_slot_id);
CREATE INDEX IF NOT EXISTS ix_contribution_byte_stable
  ON contribution (contribution_slot_id, "timestamp", id);

CREATE TABLE IF NOT EXISTS derived_contribution (
  contribution_id  uuid NOT NULL REFERENCES contribution(id) ON DELETE CASCADE,
  derived_id       uuid NOT NULL REFERENCES contribution(id) ON DELETE CASCADE,
  PRIMARY KEY (contribution_id, derived_id)
);

COMMIT;
