-- V1 - Initial Schema
-- Migrated from supabase


-- *** Enums ***

CREATE TYPE public.item_status AS ENUM ('ranked', 'backlog');


-- *** Tables ***

-- Replaces Supabase's `auth.users` (internal schema, unavailable outside Supabase).
-- All other tables FK here. `password_hash` stores a bcrypt
CREATE TABLE public.users (
  id            uuid        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  email         text        NOT NULL UNIQUE,
  password_hash text        NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- A user-defined ranking list. Each category carries `field_definitions` (JSONB array) that describe
-- the custom property fields for items in that category. `sort_order` controls the display sequence.
CREATE TABLE public.category_definitions (
  id                uuid        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  name              text        NOT NULL,
  icon              text,
  field_definitions jsonb       NOT NULL DEFAULT '[]'::jsonb,
  sort_order        integer     NOT NULL DEFAULT 0,
  user_id           uuid        NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  created_at        timestamptz NOT NULL DEFAULT now()
);

-- A rankable entry within a category. `properties` is a JSONB map of field key -> value, keyed by the stable slugified keys from `field_definitions` — the server does not validate its contents.
CREATE TABLE public.items (
  id               uuid               NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  name             text               NOT NULL,
  description      text,
  category_def_id  uuid               NOT NULL REFERENCES public.category_definitions (id) ON DELETE CASCADE,
  properties       jsonb,
  rating           numeric,
  rd               numeric            NOT NULL DEFAULT 350,
  comparison_count integer            NOT NULL DEFAULT 0,
  created_at       timestamptz        NOT NULL DEFAULT now(),
  last_compared_at timestamptz,
  status           public.item_status NOT NULL DEFAULT 'backlog',
  user_id          uuid               NOT NULL REFERENCES public.users (id) ON DELETE CASCADE
);


-- Historical record of all comparisons. Stores before/after rating values for both sides
CREATE TABLE public.comparisons (
  id                   uuid        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  created_at           timestamptz NOT NULL DEFAULT now(),
  winner_id            uuid        NOT NULL REFERENCES public.items (id) ON DELETE CASCADE,
  loser_id             uuid        NOT NULL REFERENCES public.items (id) ON DELETE CASCADE,
  winner_rating_before numeric     NOT NULL,
  winner_rating_after  numeric     NOT NULL,
  loser_rating_before  numeric     NOT NULL,
  loser_rating_after   numeric     NOT NULL,
  winner_rd_before     numeric,
  winner_rd_after      numeric,
  loser_rd_before      numeric,
  loser_rd_after       numeric,
  user_id              uuid        NOT NULL REFERENCES public.users (id) ON DELETE CASCADE
);

-- A label that can be attached to items within a category. Scoped to a single category.
-- `id` is an integer identity column (not UUID) by intentional schema design; this is
-- reflected in the Java entity as Long.
CREATE TABLE public.tags (
  id              integer NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name            text    NOT NULL,
  category_def_id uuid    NOT NULL REFERENCES public.category_definitions (id) ON DELETE CASCADE,
  user_id         uuid    NOT NULL REFERENCES public.users (id) ON DELETE CASCADE
);

-- Join table linking items to their tags. Cascades on both sides so deleting an item or a tag
-- cleans up associations automatically.
CREATE TABLE public.item_tags (
  item_id uuid    NOT NULL REFERENCES public.items (id) ON DELETE CASCADE,
  tag_id  integer NOT NULL REFERENCES public.tags (id) ON DELETE CASCADE,
  PRIMARY KEY (item_id, tag_id)
);


-- *** Indexes ***

-- Ownership lookup: every category query filters by user_id
CREATE INDEX idx_category_definitions_user_id ON public.category_definitions USING btree (user_id);

-- Primary join path: GET /categories/{id}/items always filters by category_def_id
CREATE INDEX idx_items_category_def_id  ON public.items USING btree (category_def_id);
-- Ownership check on every item write (update, delete, comparison submission)
CREATE INDEX idx_items_user_id          ON public.items USING btree (user_id);
-- Sort support: items list supports sort=createdAt
CREATE INDEX idx_items_created_at       ON public.items USING btree (created_at);
-- Sort + filter support: sort=name and filter= (name substring search)
CREATE INDEX idx_items_name             ON public.items USING btree (name);
-- Sort support: items list supports sort=rating (primary ranking display order)
CREATE INDEX idx_items_rating           ON public.items USING btree (rating);
-- GIN index on JSONB properties blob for future property-based filtering
CREATE INDEX idx_items_properties       ON public.items USING gin  (properties);

-- Ownership lookup on comparison history queries
CREATE INDEX idx_comparisons_user_id    ON public.comparisons USING btree (user_id);

-- Tag list endpoint filters by category_def_id; also used for cascade-delete checks
CREATE INDEX idx_tags_category_def_id   ON public.tags USING btree (category_def_id);
-- Ownership check when renaming or deleting a tag
CREATE INDEX idx_tags_user_id           ON public.tags USING btree (user_id);

-- Supports tag-based item filtering (tagIds query param with AND semantics)
CREATE INDEX idx_item_tags_item_id      ON public.item_tags USING btree (item_id);
-- Supports usage count queries in GET /categories/{id}/tags and merge/delete ops
CREATE INDEX idx_item_tags_tag_id       ON public.item_tags USING btree (tag_id);


-- Tracks consumed password-reset JWT IDs (jti claim) to prevent token replay.
-- A reset token is valid for 1 hour; inserting its jti here after first use ensures
-- it cannot be used a second time within that window.
-- This is a pure auth implementation detail — not a domain entity.
CREATE TABLE public.used_reset_tokens (
  id         bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  jti        text        NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Lookup on confirm: check existence before inserting
CREATE INDEX idx_used_reset_tokens_jti ON public.used_reset_tokens USING btree (jti);
-- Cleanup: periodic DELETE WHERE created_at < now() - interval '2 hours'
CREATE INDEX idx_used_reset_tokens_created_at ON public.used_reset_tokens USING btree (created_at);
