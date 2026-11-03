-- Behaviour tests: run after migration + seed (see tests/run.sh). Fails loudly on any broken expectation.
\set ON_ERROR_STOP on

insert into auth.users (id, email, raw_user_meta_data) values
  ('11111111-1111-1111-1111-111111111111', 'alice@example.com', '{"full_name":"Alice"}'),
  ('22222222-2222-2222-2222-222222222222', 'bob@example.com', '{}');

do $$ begin
  assert (select count(*) from public.profiles) = 2, 'profiles auto-created';
  assert (select display_name from public.profiles where id = '22222222-2222-2222-2222-222222222222') = 'bob',
    'display name falls back to email prefix';
  assert (select count(*) from public.whisky_flavour_tags) > 100, 'seed notes were tagged';
end $$;

-- Matching checks
do $$
declare terms text[];
begin
  select array_agg(f.term order by f.term) into terms
  from public.match_flavours('Rich sherry, dried figs, a whisper of peat smoke') m
  join public.flavour_terms f on f.id = m.term_id;
  assert terms @> array['Sherry','Dried fruit','Peat smoke'], 'basic match: ' || terms::text;

  select array_agg(f.term) into terms
  from public.match_flavours('hint of vanila and buterscotch') m join public.flavour_terms f on f.id = m.term_id;
  assert terms @> array['Vanilla','Toffee & caramel'], 'misspellings: ' || coalesce(terms::text, 'none');
end $$;

-- Act as Alice
set role authenticated;
select set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', false);

do $$
declare v_whisky uuid; v_event uuid; v_contact uuid; v_tasting uuid; n int;
begin
  select id into v_whisky from public.search_whiskies('lagavulin 16') limit 1;
  assert (select name from public.whiskies where id = v_whisky) = 'Lagavulin 16 Year Old', 'search finds Lagavulin';
  assert (select name from public.search_whiskies('lag 16') limit 1) = 'Lagavulin 16 Year Old', 'alias search';
  assert (select name from public.search_whiskies('glenfidich') limit 1) = 'Glenfiddich 12 Year Old', 'fuzzy search';
  assert (select count(*) from public.flavour_suggestions(v_whisky)) >= 5, 'suggestions exist';
  assert (select term from public.search_flavours('choc') limit 1) = 'Chocolate', 'flavour autocomplete';

  -- age gate blocks tastings until confirmed
  begin
    perform public.save_tasting(jsonb_build_object('whisky_id', v_whisky));
    raise exception 'age gate did not block';
  exception when insufficient_privilege then null;  -- RLS violation
  end;

  update public.profiles set over_18_confirmed_at = now() where id = auth.uid();

  insert into public.events (name, venue) values ('Whisky Live Edinburgh', 'Assembly Rooms') returning id into v_event;
  insert into public.brand_contacts (name, brand_or_company, role) values ('Sam', 'Lagavulin', 'Brand ambassador') returning id into v_contact;

  v_tasting := public.save_tasting(jsonb_build_object(
    'whisky_id', v_whisky, 'tasted_at', '2026-11-01T19:30:00Z', 'location_name', 'Edinburgh',
    'event_id', v_event, 'brand_contact_id', v_contact, 'rating', 5, 'would_buy', true,
    'flavours', jsonb_build_array(
      jsonb_build_object('term_id', 38, 'phase', 'nose', 'response', 'confirmed', 'intensity', 3),
      jsonb_build_object('term_id', 40, 'phase', 'nose', 'response', 'rejected'),
      jsonb_build_object('term_id', 24, 'phase', 'palate', 'response', 'confirmed', 'was_suggested', false))));

  assert (select count(*) from public.tasting_flavours where tasting_id = v_tasting) = 3, 'flavours saved';
  assert (select event_name from public.tasting_summaries where id = v_tasting) = 'Whisky Live Edinburgh', 'summary view';
  assert (select score from public.user_flavour_profile where term_id = 38) = 3.00, 'profile score peat';
  assert (select score from public.user_flavour_profile where term_id = 40) = -0.50, 'profile score rejected';

  -- unverified whisky path
  v_whisky := public.add_unverified_whisky('Mystery Cask 2019', 'Tiny Island Distillery', null, 58.2);
  assert (select verified from public.whiskies where id = v_whisky) = false, 'unverified whisky';

  -- catalogue is not directly writable
  begin
    insert into public.whiskies (name) values ('Hack');
    raise exception 'catalogue was writable';
  exception when insufficient_privilege then null;
  end;
end $$;

-- Act as Bob: must not see Alice's data
select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', false);
do $$ begin
  assert (select count(*) from public.tastings) = 0, 'bob cannot see alice tastings';
  assert (select count(*) from public.events) = 0, 'bob cannot see alice events';
  assert (select count(*) from public.brand_contacts) = 0, 'bob cannot see alice contacts';
  assert (select count(*) from public.tasting_summaries) = 0, 'bob summary empty';
  assert (select count(*) from public.user_flavour_profile) = 0, 'bob profile empty';
  assert (select count(*) from public.profiles) = 1, 'bob sees only his profile';
end $$;

reset role;
-- Account deletion cascades
delete from auth.users where id = '11111111-1111-1111-1111-111111111111';
do $$ begin
  assert (select count(*) from public.tastings) = 0, 'tastings cascade-deleted';
  assert (select count(*) from public.brand_contacts) = 0, 'contacts cascade-deleted';
  assert (select count(*) from public.whiskies where name = 'Mystery Cask 2019' and created_by_user_id is null) = 1,
    'user-submitted whisky kept, owner cleared';
end $$;

select 'ALL TESTS PASSED' as result;
