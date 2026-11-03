-- Whisky Journal — MVP schema (slices 1–3: auth/profile/settings, catalogue + tastings, flavour confirmation)
-- Apply in the Supabase SQL editor, or with `supabase db push` from the repo root.

create extension if not exists pg_trgm with schema extensions;

-- ---------------------------------------------------------------------------
-- Helpers
-- ---------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end $$;

-- ---------------------------------------------------------------------------
-- 4.1 User & preferences
-- ---------------------------------------------------------------------------
create table public.profiles (
  id                    uuid primary key references auth.users(id) on delete cascade,
  display_name          text,
  over_18_confirmed_at  timestamptz,
  country               text default 'GB',
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now()
);

create table public.user_settings (
  user_id                 uuid primary key references auth.users(id) on delete cascade,
  share_location_default  boolean not null default false,
  units                   text not null default 'ml' check (units in ('ml', 'cl')),
  notifications_enabled   boolean not null default false,
  updated_at              timestamptz not null default now()
);

create trigger profiles_updated_at before update on public.profiles
  for each row execute function public.set_updated_at();
create trigger user_settings_updated_at before update on public.user_settings
  for each row execute function public.set_updated_at();

-- Create profile + settings rows automatically when someone signs up.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'full_name',
                           new.raw_user_meta_data ->> 'name',
                           split_part(new.email, '@', 1)));
  insert into public.user_settings (user_id) values (new.id);
  return new;
end $$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- 4.2 Catalogue (shared, read-only to users except unverified submissions)
-- ---------------------------------------------------------------------------
create table public.distilleries (
  id                  uuid primary key default gen_random_uuid(),
  name                text not null,
  region              text,
  country             text,
  owner               text,
  website             text,
  verified            boolean not null default true,
  created_by_user_id  uuid references auth.users(id) on delete set null,
  created_at          timestamptz not null default now()
);
create unique index distilleries_name_key on public.distilleries (lower(name));
create index distilleries_name_trgm on public.distilleries using gin (name extensions.gin_trgm_ops);

create table public.whiskies (
  id                  uuid primary key default gen_random_uuid(),
  distillery_id       uuid references public.distilleries(id) on delete set null,
  name                text not null,
  age_years           smallint check (age_years between 0 and 80),  -- null = no age statement
  abv                 numeric(4,1) check (abv between 0 and 100),
  cask_type           text,
  bottler             text check (bottler in ('official', 'independent')),
  edition             text,
  style               text,  -- single malt, blended malt, blend, single pot still, bourbon…
  verified            boolean not null default true,
  created_by_user_id  uuid references auth.users(id) on delete set null,
  created_at          timestamptz not null default now()
);
create index whiskies_name_trgm on public.whiskies using gin (name extensions.gin_trgm_ops);
create index whiskies_distillery on public.whiskies (distillery_id);

create table public.whisky_aliases (
  whisky_id  uuid not null references public.whiskies(id) on delete cascade,
  alias      text not null,
  primary key (whisky_id, alias)
);
create index whisky_aliases_trgm on public.whisky_aliases using gin (alias extensions.gin_trgm_ops);

create table public.flavour_terms (
  id         serial primary key,
  term       text not null unique,
  category   text not null,
  parent_id  int references public.flavour_terms(id)
);

create table public.flavour_synonyms (
  term_id  int not null references public.flavour_terms(id) on delete cascade,
  synonym  text not null,
  primary key (term_id, synonym)
);
create index flavour_synonyms_trgm on public.flavour_synonyms using gin (synonym extensions.gin_trgm_ops);

create table public.whisky_tasting_notes (
  id          uuid primary key default gen_random_uuid(),
  whisky_id   uuid not null references public.whiskies(id) on delete cascade,
  phase       text not null check (phase in ('nose', 'palate', 'finish')),
  raw_text    text not null,
  source      text not null default 'curated'
                check (source in ('distillery', 'retailer', 'curated', 'curated-sample', 'llm-unverified')),
  source_url  text,
  created_at  timestamptz not null default now()
);

create table public.whisky_flavour_tags (
  whisky_id       uuid not null references public.whiskies(id) on delete cascade,
  term_id         int  not null references public.flavour_terms(id) on delete cascade,
  phase           text not null check (phase in ('nose', 'palate', 'finish')),
  confidence      real not null check (confidence between 0 and 1),
  source_note_id  uuid references public.whisky_tasting_notes(id) on delete cascade,
  primary key (whisky_id, term_id, phase)
);

-- ---------------------------------------------------------------------------
-- 4.5 Context: events & brand contacts (private to the user)
-- ---------------------------------------------------------------------------
create table public.events (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name        text not null,
  venue       text,
  start_date  date,
  end_date    date,
  created_at  timestamptz not null default now()
);
create index events_user on public.events (user_id);

create table public.brand_contacts (
  id                uuid primary key default gen_random_uuid(),
  user_id           uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name              text not null,
  brand_or_company  text,
  role              text,
  notes             text,
  created_at        timestamptz not null default now()
);
create index brand_contacts_user on public.brand_contacts (user_id);

-- ---------------------------------------------------------------------------
-- 4.4 Tastings
-- ---------------------------------------------------------------------------
create table public.tastings (
  id                uuid primary key default gen_random_uuid(),
  user_id           uuid not null default auth.uid() references auth.users(id) on delete cascade,
  whisky_id         uuid not null references public.whiskies(id),
  tasted_at         timestamptz not null default now(),
  location_name     text,
  latitude          double precision,
  longitude         double precision,
  event_id          uuid references public.events(id) on delete set null,
  brand_contact_id  uuid references public.brand_contacts(id) on delete set null,
  rating            smallint check (rating between 1 and 5),
  would_buy         boolean,
  personal_notes    text,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now()
);
create index tastings_user_time on public.tastings (user_id, tasted_at desc);
create trigger tastings_updated_at before update on public.tastings
  for each row execute function public.set_updated_at();

create table public.tasting_flavours (
  tasting_id     uuid not null references public.tastings(id) on delete cascade,
  term_id        int  not null references public.flavour_terms(id),
  phase          text not null check (phase in ('nose', 'palate', 'finish')),
  response       text not null check (response in ('confirmed', 'rejected', 'unsure')),
  intensity      smallint check (intensity between 1 and 3),
  was_suggested  boolean not null default true,
  primary key (tasting_id, term_id, phase)
);

-- ---------------------------------------------------------------------------
-- Row-level security
-- ---------------------------------------------------------------------------
alter table public.profiles             enable row level security;
alter table public.user_settings        enable row level security;
alter table public.distilleries         enable row level security;
alter table public.whiskies             enable row level security;
alter table public.whisky_aliases       enable row level security;
alter table public.flavour_terms        enable row level security;
alter table public.flavour_synonyms     enable row level security;
alter table public.whisky_tasting_notes enable row level security;
alter table public.whisky_flavour_tags  enable row level security;
alter table public.events               enable row level security;
alter table public.brand_contacts       enable row level security;
alter table public.tastings             enable row level security;
alter table public.tasting_flavours     enable row level security;

-- Own profile & settings
create policy "own profile" on public.profiles
  for all to authenticated using (id = auth.uid()) with check (id = auth.uid());
create policy "own settings" on public.user_settings
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Catalogue: readable by any signed-in user (writes go through add_unverified_whisky or service role)
create policy "read catalogue" on public.distilleries         for select to authenticated using (true);
create policy "read catalogue" on public.whiskies             for select to authenticated using (true);
create policy "read catalogue" on public.whisky_aliases       for select to authenticated using (true);
create policy "read catalogue" on public.flavour_terms        for select to authenticated using (true);
create policy "read catalogue" on public.flavour_synonyms     for select to authenticated using (true);
create policy "read catalogue" on public.whisky_tasting_notes for select to authenticated using (true);
create policy "read catalogue" on public.whisky_flavour_tags  for select to authenticated using (true);

-- Private context
create policy "own events" on public.events
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "own contacts" on public.brand_contacts
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Tastings: own rows only, and only once the age gate is confirmed
create or replace function public.is_age_confirmed()
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.profiles
                 where id = auth.uid() and over_18_confirmed_at is not null);
$$;

create policy "own tastings read" on public.tastings
  for select to authenticated using (user_id = auth.uid());
create policy "own tastings write" on public.tastings
  for insert to authenticated with check (user_id = auth.uid() and public.is_age_confirmed());
create policy "own tastings update" on public.tastings
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "own tastings delete" on public.tastings
  for delete to authenticated using (user_id = auth.uid());

create policy "own tasting flavours" on public.tasting_flavours
  for all to authenticated
  using (exists (select 1 from public.tastings t where t.id = tasting_id and t.user_id = auth.uid()))
  with check (exists (select 1 from public.tastings t where t.id = tasting_id and t.user_id = auth.uid()));

-- ---------------------------------------------------------------------------
-- Fuzzy matching: tasting note text -> flavour terms
-- ---------------------------------------------------------------------------
-- Splits a note into phrases, strips qualifiers ("a hint of"), then matches each
-- phrase against flavour synonyms: whole-word match (confidence 1.0), otherwise
-- trigram similarity for misspellings and variants (confidence = similarity).
create or replace function public.match_flavours(p_text text, p_threshold real default 0.45)
returns table (term_id int, confidence real, matched_phrase text)
language sql stable set search_path = public, extensions as $$
  with phrases as (
    select distinct trim(regexp_replace(lower(p),
             '^(a |an |the |some |hints? of |touch of |whisper of |notes? of |lots of |plenty of |'
             || 'light |lightly |gentle |subtle |soft |rich |fresh |sweet |dark |hint of |little |faint )+', '')) as phrase
    from regexp_split_to_table(lower(p_text), '\s*(,|;|\.|\yand\y|\ywith\y|\ythen\y|\yfollowed by\y|&)\s*') as p
  ), candidates as (
    select s.term_id, 1.0::real as confidence, s.synonym as phrase
    from flavour_synonyms s
    where lower(p_text) ~ ('\m' || s.synonym || '\M')
    union all
    select s.term_id, similarity(ph.phrase, s.synonym)::real, ph.phrase
    from phrases ph
    join flavour_synonyms s on ph.phrase % s.synonym
    where length(ph.phrase) > 2 and similarity(ph.phrase, s.synonym) >= p_threshold
      -- fuzzy only for phrases that contain no exact synonym (stops "smoke" also fuzzing to "wood smoke")
      and not exists (select 1 from flavour_synonyms x where ph.phrase ~ ('\m' || x.synonym || '\M'))
  )
  select distinct on (term_id) term_id, confidence, phrase
  from candidates
  order by term_id, confidence desc;
$$;

create or replace function public.tag_tasting_note()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into whisky_flavour_tags (whisky_id, term_id, phase, confidence, source_note_id)
  select new.whisky_id, m.term_id, new.phase, m.confidence, new.id
  from match_flavours(new.raw_text) m
  on conflict (whisky_id, term_id, phase)
    do update set confidence = greatest(whisky_flavour_tags.confidence, excluded.confidence);
  return new;
end $$;

create trigger whisky_tasting_notes_tag
  after insert or update of raw_text on public.whisky_tasting_notes
  for each row execute function public.tag_tasting_note();

-- ---------------------------------------------------------------------------
-- RPCs used by the app
-- ---------------------------------------------------------------------------
create or replace function public.search_whiskies(q text, max_results int default 20)
returns table (id uuid, name text, distillery text, region text, age_years smallint,
               abv numeric, style text, verified boolean, score real)
language sql stable set search_path = public, extensions as $$
  with scored as (
    select w.id,
           greatest(
             word_similarity(lower(q), lower(w.name)),
             word_similarity(lower(q), lower(coalesce(d.name, '') || ' ' || w.name)),
             coalesce((select max(word_similarity(lower(q), lower(a.alias)))
                       from whisky_aliases a where a.whisky_id = w.id), 0)
           )::real as score
    from whiskies w
    left join distilleries d on d.id = w.distillery_id
  )
  select w.id, w.name, d.name, d.region, w.age_years, w.abv, w.style, w.verified, s.score
  from scored s
  join whiskies w on w.id = s.id
  left join distilleries d on d.id = w.distillery_id
  where coalesce(trim(q), '') = '' or s.score >= 0.3
  order by s.score desc, w.verified desc, w.name
  limit max_results;
$$;

create or replace function public.flavour_suggestions(p_whisky_id uuid)
returns table (term_id int, term text, category text, phase text, confidence real)
language sql stable set search_path = public as $$
  select t.term_id, f.term, f.category, t.phase, t.confidence
  from whisky_flavour_tags t
  join flavour_terms f on f.id = t.term_id
  where t.whisky_id = p_whisky_id
  order by case t.phase when 'nose' then 1 when 'palate' then 2 else 3 end, t.confidence desc, f.term;
$$;

create or replace function public.search_flavours(q text, max_results int default 10)
returns table (term_id int, term text, category text, score real)
language sql stable set search_path = public, extensions as $$
  select f.id, f.term, f.category,
         max(greatest(word_similarity(lower(q), s.synonym), word_similarity(lower(q), lower(f.term))))::real as score
  from flavour_terms f
  left join flavour_synonyms s on s.term_id = f.id
  group by f.id, f.term, f.category
  having max(greatest(word_similarity(lower(q), s.synonym), word_similarity(lower(q), lower(f.term)))) >= 0.3
  order by score desc, f.term
  limit max_results;
$$;

-- "None of these": lets a user add a bottle that isn't in the catalogue yet.
-- Finds or creates the distillery, inserts an unverified whisky owned by the caller.
create or replace function public.add_unverified_whisky(
  p_name text, p_distillery text default null, p_age_years smallint default null, p_abv numeric default null)
returns uuid language plpgsql security definer set search_path = public as $$
declare
  v_distillery uuid;
  v_whisky uuid;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if coalesce(trim(p_name), '') = '' then raise exception 'name is required'; end if;

  if coalesce(trim(p_distillery), '') <> '' then
    select id into v_distillery from distilleries where lower(name) = lower(trim(p_distillery));
    if v_distillery is null then
      insert into distilleries (name, verified, created_by_user_id)
      values (trim(p_distillery), false, auth.uid()) returning id into v_distillery;
    end if;
  end if;

  insert into whiskies (distillery_id, name, age_years, abv, verified, created_by_user_id)
  values (v_distillery, trim(p_name), p_age_years, p_abv, false, auth.uid())
  returning id into v_whisky;
  return v_whisky;
end $$;

-- Saves a tasting and its flavour responses in one transaction.
-- payload: { whisky_id, tasted_at, location_name, event_id, brand_contact_id, rating, would_buy,
--            personal_notes, flavours: [{ term_id, phase, response, intensity, was_suggested }] }
create or replace function public.save_tasting(payload jsonb)
returns uuid language plpgsql security invoker set search_path = public as $$
declare
  v_id uuid;
begin
  insert into tastings (user_id, whisky_id, tasted_at, location_name, latitude, longitude,
                        event_id, brand_contact_id, rating, would_buy, personal_notes)
  values (auth.uid(),
          (payload ->> 'whisky_id')::uuid,
          coalesce((payload ->> 'tasted_at')::timestamptz, now()),
          nullif(payload ->> 'location_name', ''),
          (payload ->> 'latitude')::double precision,
          (payload ->> 'longitude')::double precision,
          (payload ->> 'event_id')::uuid,
          (payload ->> 'brand_contact_id')::uuid,
          (payload ->> 'rating')::smallint,
          (payload ->> 'would_buy')::boolean,
          nullif(payload ->> 'personal_notes', ''))
  returning id into v_id;

  insert into tasting_flavours (tasting_id, term_id, phase, response, intensity, was_suggested)
  select v_id, (f ->> 'term_id')::int, f ->> 'phase', f ->> 'response',
         (f ->> 'intensity')::smallint, coalesce((f ->> 'was_suggested')::boolean, true)
  from jsonb_array_elements(coalesce(payload -> 'flavours', '[]'::jsonb)) f
  on conflict do nothing;

  return v_id;
end $$;

-- Tasting history in a shape the list screen can use directly.
create or replace view public.tasting_summaries with (security_invoker = true) as
select t.id, t.tasted_at, t.rating, t.would_buy, t.location_name,
       w.id as whisky_id, w.name as whisky_name, w.age_years, d.name as distillery,
       e.name as event_name, c.name as brand_contact_name, c.brand_or_company,
       (select count(*) from tasting_flavours tf
         where tf.tasting_id = t.id and tf.response = 'confirmed') as confirmed_flavours
from tastings t
join whiskies w on w.id = t.whisky_id
left join distilleries d on d.id = w.distillery_id
left join events e on e.id = t.event_id
left join brand_contacts c on c.id = t.brand_contact_id;

-- 4.7 Flavour profile (computed on read; RLS on tastings scopes it to the caller).
-- confirmed: weight by rating (5★ +2 … 1★ −1) scaled by intensity; rejected −0.5; unsure 0.
create or replace view public.user_flavour_profile with (security_invoker = true) as
select t.user_id, f.id as term_id, f.term, f.category,
       round(sum(case tf.response
             when 'confirmed' then
               (case t.rating when 5 then 2 when 4 then 1.5 when 3 then 1 when 2 then 0 when 1 then -1 else 1 end)
               * coalesce(tf.intensity, 2) / 2.0
             when 'rejected' then -0.5
             else 0 end)::numeric, 2) as score,
       count(distinct t.id) as tasting_count
from tasting_flavours tf
join tastings t on t.id = tf.tasting_id
join flavour_terms f on f.id = tf.term_id
group by t.user_id, f.id, f.term, f.category;

grant select on public.tasting_summaries, public.user_flavour_profile to authenticated;
grant execute on function public.search_whiskies(text, int), public.flavour_suggestions(uuid),
  public.search_flavours(text, int), public.add_unverified_whisky(text, text, smallint, numeric),
  public.save_tasting(jsonb), public.match_flavours(text, real) to authenticated;
revoke execute on function public.add_unverified_whisky(text, text, smallint, numeric) from anon, public;
revoke execute on function public.save_tasting(jsonb) from anon, public;
