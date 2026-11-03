-- Whisky Journal — seed data for development.
-- Flavour vocabulary is intended to be kept and extended.
-- The whisky catalogue and tasting notes below are SAMPLE data written for testing
-- (source = 'curated-sample'). ABVs vary by market — check before relying on them,
-- and replace with your own curated or licensed catalogue before launch.

-- ---------------------------------------------------------------------------
-- Flavour vocabulary: categories (parent_id null) and terms beneath them
-- ---------------------------------------------------------------------------
insert into public.flavour_terms (id, term, category, parent_id) values
  (1,  'Fruity',               'Fruity',              null),
  (2,  'Floral',               'Floral',              null),
  (3,  'Sweet',                'Sweet',               null),
  (4,  'Spicy',                'Spicy',               null),
  (5,  'Woody',                'Woody',               null),
  (6,  'Cereal',               'Cereal',              null),
  (7,  'Peaty & smoky',        'Peaty & smoky',       null),
  (8,  'Maritime',             'Maritime',            null),
  (9,  'Nutty',                'Nutty',               null),
  (10, 'Winey & sherried',     'Winey & sherried',    null),
  (11, 'Citrus',               'Fruity',              1),
  (12, 'Orchard fruit',        'Fruity',              1),
  (13, 'Stone fruit',          'Fruity',              1),
  (14, 'Tropical fruit',       'Fruity',              1),
  (15, 'Dried fruit',          'Fruity',              1),
  (16, 'Berries',              'Fruity',              1),
  (17, 'Flowers',              'Floral',              2),
  (18, 'Heather',              'Floral',              2),
  (19, 'Grassy',               'Floral',              2),
  (20, 'Herbal',               'Floral',              2),
  (21, 'Vanilla',              'Sweet',               3),
  (22, 'Honey',                'Sweet',               3),
  (23, 'Toffee & caramel',     'Sweet',               3),
  (24, 'Chocolate',            'Sweet',               3),
  (25, 'Brown sugar',          'Sweet',               3),
  (26, 'Cinnamon',             'Spicy',               4),
  (27, 'Black pepper',         'Spicy',               4),
  (28, 'Ginger',               'Spicy',               4),
  (29, 'Baking spice',         'Spicy',               4),
  (30, 'Liquorice & aniseed',  'Spicy',               4),
  (31, 'Oak',                  'Woody',               5),
  (32, 'Coconut',              'Woody',               5),
  (33, 'Tobacco & leather',    'Woody',               5),
  (34, 'Coffee',               'Woody',               5),
  (35, 'Earthy',               'Woody',               5),
  (36, 'Malt',                 'Cereal',              6),
  (37, 'Bread & biscuit',      'Cereal',              6),
  (38, 'Peat smoke',           'Peaty & smoky',       7),
  (39, 'Bonfire & ash',        'Peaty & smoky',       7),
  (40, 'Medicinal',            'Peaty & smoky',       7),
  (41, 'Smoked meat',          'Peaty & smoky',       7),
  (42, 'Sea salt',             'Maritime',            8),
  (43, 'Seaweed',              'Maritime',            8),
  (44, 'Almond',               'Nutty',               9),
  (45, 'Walnut & hazelnut',    'Nutty',               9),
  (46, 'Sherry',               'Winey & sherried',    10),
  (47, 'Red wine & port',      'Winey & sherried',    10);

select setval('public.flavour_terms_id_seq', 100);

insert into public.flavour_synonyms (term_id, synonym)
select v.term_id, s
from (values
  (11, array['citrus','lemon','orange','orange peel','lime','grapefruit','zest','marmalade','tangerine']),
  (12, array['apple','green apple','baked apple','pear','poached pear','orchard fruit']),
  (13, array['peach','apricot','plum','cherry','nectarine','stone fruit']),
  (14, array['pineapple','mango','banana','passion fruit','tropical fruit','tropical']),
  (15, array['raisin','raisins','sultana','sultanas','fig','figs','date','dates','prune','prunes','dried fruit','christmas cake','fruitcake','fruit cake']),
  (16, array['berries','strawberry','raspberry','blackcurrant','red berries','bramble','blackberry']),
  (17, array['floral','flowers','rose','violet','lavender','blossom','orange blossom']),
  (18, array['heather','heathery']),
  (19, array['grass','grassy','cut grass','hay','straw']),
  (20, array['herbal','herbs','mint','eucalyptus','menthol','pine']),
  (21, array['vanilla','vanilla pod','custard','cream','creamy']),
  (22, array['honey','honeyed','heather honey','mead','beeswax']),
  (23, array['toffee','caramel','butterscotch','fudge','treacle','salted caramel']),
  (24, array['chocolate','dark chocolate','milk chocolate','cocoa']),
  (25, array['brown sugar','muscovado','demerara','molasses','maple','syrup']),
  (26, array['cinnamon']),
  (27, array['pepper','black pepper','peppery','white pepper','chilli']),
  (28, array['ginger','stem ginger']),
  (29, array['spice','spices','spicy','baking spice','mixed spice','clove','cloves','nutmeg','allspice']),
  (30, array['liquorice','licorice','aniseed','anise','fennel']),
  (31, array['oak','oaky','wood','woody','cedar','sawdust','pencil shavings','toasted oak']),
  (32, array['coconut','toasted coconut']),
  (33, array['tobacco','leather','cigar box','cigar']),
  (34, array['coffee','espresso','mocha']),
  (35, array['earthy','mushroom','damp earth','forest floor','moss']),
  (36, array['malt','malty','barley','cereal','grain','wort']),
  (37, array['bread','biscuit','biscuits','digestive','shortbread','toast','pastry','brioche','oatmeal','porridge']),
  (38, array['peat','peaty','peat smoke','smoke','smoky','smoked','peated']),
  (39, array['bonfire','ash','charcoal','embers','wood smoke','campfire','soot']),
  (40, array['iodine','medicinal','tcp','antiseptic','bandages','tar','rubber']),
  (41, array['bacon','smoked meat','ham','barbecue','bbq']),
  (42, array['salt','salty','sea salt','brine','briny','sea spray','saline','maritime','coastal']),
  (43, array['seaweed','kelp','rock pools','oyster','oysters']),
  (44, array['almond','almonds','marzipan','amaretto']),
  (45, array['walnut','walnuts','hazelnut','hazelnuts','nutty','nuts','pecan']),
  (46, array['sherry','sherried','oloroso','px','pedro ximenez','sherry cask']),
  (47, array['port','red wine','wine','winey'])
) as v(term_id, syns), unnest(v.syns) as s;

-- ---------------------------------------------------------------------------
-- Sample catalogue
-- ---------------------------------------------------------------------------
insert into public.distilleries (name, region, country) values
  ('Glenfiddich',   'Speyside',    'Scotland'),
  ('The Glenlivet', 'Speyside',    'Scotland'),
  ('The Macallan',  'Speyside',    'Scotland'),
  ('The Balvenie',  'Speyside',    'Scotland'),
  ('Lagavulin',     'Islay',       'Scotland'),
  ('Laphroaig',     'Islay',       'Scotland'),
  ('Ardbeg',        'Islay',       'Scotland'),
  ('Bowmore',       'Islay',       'Scotland'),
  ('Caol Ila',      'Islay',       'Scotland'),
  ('Talisker',      'Islands',     'Scotland'),
  ('Highland Park', 'Islands',     'Scotland'),
  ('Oban',          'Highlands',   'Scotland'),
  ('The Dalmore',   'Highlands',   'Scotland'),
  ('Glenmorangie',  'Highlands',   'Scotland'),
  ('GlenDronach',   'Highlands',   'Scotland'),
  ('Springbank',    'Campbeltown', 'Scotland'),
  ('Auchentoshan',  'Lowlands',    'Scotland'),
  ('Glenkinchie',   'Lowlands',    'Scotland'),
  ('Midleton',      'Cork',        'Ireland'),
  ('Buffalo Trace', 'Kentucky',    'USA');

insert into public.whiskies (distillery_id, name, age_years, abv, style, bottler)
select d.id, v.name, v.age, v.abv, v.style, 'official'
from (values
  ('Glenfiddich',   'Glenfiddich 12 Year Old',              12::smallint, 40.0, 'Single malt'),
  ('The Glenlivet', 'The Glenlivet 12 Year Old',            12,  40.0, 'Single malt'),
  ('The Macallan',  'The Macallan 12 Year Old Sherry Oak',  12,  40.0, 'Single malt'),
  ('The Balvenie',  'The Balvenie DoubleWood 12 Year Old',  12,  40.0, 'Single malt'),
  ('Lagavulin',     'Lagavulin 16 Year Old',                16,  43.0, 'Single malt'),
  ('Laphroaig',     'Laphroaig 10 Year Old',                10,  40.0, 'Single malt'),
  ('Ardbeg',        'Ardbeg Ten',                           10,  46.0, 'Single malt'),
  ('Bowmore',       'Bowmore 12 Year Old',                  12,  40.0, 'Single malt'),
  ('Caol Ila',      'Caol Ila 12 Year Old',                 12,  43.0, 'Single malt'),
  ('Talisker',      'Talisker 10 Year Old',                 10,  45.8, 'Single malt'),
  ('Highland Park', 'Highland Park 12 Year Old Viking Honour', 12, 40.0, 'Single malt'),
  ('Oban',          'Oban 14 Year Old',                     14,  43.0, 'Single malt'),
  ('The Dalmore',   'The Dalmore 12 Year Old',              12,  40.0, 'Single malt'),
  ('Glenmorangie',  'Glenmorangie The Original',            10,  40.0, 'Single malt'),
  ('GlenDronach',   'The GlenDronach 12 Year Old',          12,  43.0, 'Single malt'),
  ('Springbank',    'Springbank 10 Year Old',               10,  46.0, 'Single malt'),
  ('Auchentoshan',  'Auchentoshan Three Wood',              null, 43.0, 'Single malt'),
  ('Glenkinchie',   'Glenkinchie 12 Year Old',              12,  43.0, 'Single malt'),
  ('Midleton',      'Redbreast 12 Year Old',                12,  40.0, 'Single pot still'),
  ('Buffalo Trace', 'Buffalo Trace Kentucky Straight Bourbon', null, 40.0, 'Bourbon')
) as v(dist, name, age, abv, style)
join public.distilleries d on d.name = v.dist;

insert into public.whisky_aliases (whisky_id, alias)
select w.id, v.alias
from (values
  ('Lagavulin 16 Year Old', 'Lag 16'),
  ('Ardbeg Ten', 'Ardbeg 10'),
  ('Glenmorangie The Original', 'Glenmorangie 10'),
  ('The Macallan 12 Year Old Sherry Oak', 'Macallan 12'),
  ('The Balvenie DoubleWood 12 Year Old', 'Balvenie 12'),
  ('Highland Park 12 Year Old Viking Honour', 'Highland Park 12'),
  ('Redbreast 12 Year Old', 'Redbreast 12')
) as v(wname, alias)
join public.whiskies w on w.name = v.wname;

-- Tasting notes (sample descriptors). Inserting them fires the flavour-tagging trigger.
insert into public.whisky_tasting_notes (whisky_id, phase, raw_text, source)
select w.id, v.phase, v.txt, 'curated-sample'
from (values
  ('Glenfiddich 12 Year Old', 'nose',   'Fresh pear, green apple and a hint of oak'),
  ('Glenfiddich 12 Year Old', 'palate', 'Butterscotch, cream and malt'),
  ('Glenfiddich 12 Year Old', 'finish', 'Gentle, with a touch of oak'),
  ('The Glenlivet 12 Year Old', 'nose',   'Floral notes, pineapple and citrus'),
  ('The Glenlivet 12 Year Old', 'palate', 'Vanilla, honey and summer fruit'),
  ('The Glenlivet 12 Year Old', 'finish', 'Soft, with almond and a little spice'),
  ('The Macallan 12 Year Old Sherry Oak', 'nose',   'Dried fruit, sherry and vanilla'),
  ('The Macallan 12 Year Old Sherry Oak', 'palate', 'Rich raisins, ginger and wood smoke'),
  ('The Macallan 12 Year Old Sherry Oak', 'finish', 'Toffee, dried fruit and a hint of spice'),
  ('The Balvenie DoubleWood 12 Year Old', 'nose',   'Honey, vanilla and sherried fruit'),
  ('The Balvenie DoubleWood 12 Year Old', 'palate', 'Nutty, with cinnamon and honey'),
  ('The Balvenie DoubleWood 12 Year Old', 'finish', 'Long and warming, with oak'),
  ('Lagavulin 16 Year Old', 'nose',   'Intense peat smoke, iodine and seaweed'),
  ('Lagavulin 16 Year Old', 'palate', 'Sherry sweetness, dried fruit and smoke'),
  ('Lagavulin 16 Year Old', 'finish', 'Long, with peat, salt and figs'),
  ('Laphroaig 10 Year Old', 'nose',   'Medicinal smoke, seaweed and a touch of vanilla'),
  ('Laphroaig 10 Year Old', 'palate', 'Peat, sea salt and a hint of sweetness'),
  ('Laphroaig 10 Year Old', 'finish', 'Long, with ash and iodine'),
  ('Ardbeg Ten', 'nose',   'Smoke, lemon zest and tar'),
  ('Ardbeg Ten', 'palate', 'Peat, black pepper, toffee and espresso'),
  ('Ardbeg Ten', 'finish', 'Long and smoky, with aniseed'),
  ('Bowmore 12 Year Old', 'nose',   'Gentle peat smoke, citrus and honey'),
  ('Bowmore 12 Year Old', 'palate', 'Dark chocolate, lemon and smoke'),
  ('Bowmore 12 Year Old', 'finish', 'Salty, with lingering smoke'),
  ('Caol Ila 12 Year Old', 'nose',   'Light smoke, cut grass and lemon'),
  ('Caol Ila 12 Year Old', 'palate', 'Peat, oily, with pear and sea salt'),
  ('Caol Ila 12 Year Old', 'finish', 'Smoky and peppery'),
  ('Talisker 10 Year Old', 'nose',   'Sea spray, peat smoke and dried fruit'),
  ('Talisker 10 Year Old', 'palate', 'Black pepper, barley and smoke'),
  ('Talisker 10 Year Old', 'finish', 'Long and peppery with salt'),
  ('Highland Park 12 Year Old Viking Honour', 'nose',   'Heather honey and gentle smoke'),
  ('Highland Park 12 Year Old Viking Honour', 'palate', 'Orange, toffee and a hint of peat'),
  ('Highland Park 12 Year Old Viking Honour', 'finish', 'Medium, with smoke and spice'),
  ('Oban 14 Year Old', 'nose',   'Orange peel, sea salt and a whisper of smoke'),
  ('Oban 14 Year Old', 'palate', 'Honey, dried figs and malt'),
  ('Oban 14 Year Old', 'finish', 'Long and smooth, with oak'),
  ('The Dalmore 12 Year Old', 'nose',   'Orange marmalade, chocolate and sherry'),
  ('The Dalmore 12 Year Old', 'palate', 'Citrus, vanilla and cinnamon'),
  ('The Dalmore 12 Year Old', 'finish', 'Coffee, chocolate and spice'),
  ('Glenmorangie The Original', 'nose',   'Citrus, peach and vanilla'),
  ('Glenmorangie The Original', 'palate', 'Vanilla, honey and orange blossom'),
  ('Glenmorangie The Original', 'finish', 'Clean, with apple and nutmeg'),
  ('The GlenDronach 12 Year Old', 'nose',   'Sherry, raisins and cinnamon'),
  ('The GlenDronach 12 Year Old', 'palate', 'Christmas cake, walnut and chocolate'),
  ('The GlenDronach 12 Year Old', 'finish', 'Long, with dried fruit and oak'),
  ('Springbank 10 Year Old', 'nose',   'Pear, malt and a touch of peat'),
  ('Springbank 10 Year Old', 'palate', 'Brine, vanilla, nutmeg and smoke'),
  ('Springbank 10 Year Old', 'finish', 'Salty and long, with oak'),
  ('Auchentoshan Three Wood', 'nose',   'Brown sugar, raisins and orange'),
  ('Auchentoshan Three Wood', 'palate', 'Toffee, plum and hazelnut'),
  ('Auchentoshan Three Wood', 'finish', 'Sweet, with oak and spice'),
  ('Glenkinchie 12 Year Old', 'nose',   'Fresh grass, flowers and lemon'),
  ('Glenkinchie 12 Year Old', 'palate', 'Malt, honey and apple'),
  ('Glenkinchie 12 Year Old', 'finish', 'Short and dry with a little spice'),
  ('Redbreast 12 Year Old', 'nose',   'Dried fruit, sherry and ginger'),
  ('Redbreast 12 Year Old', 'palate', 'Creamy, with toasted oak, apple and spice'),
  ('Redbreast 12 Year Old', 'finish', 'Long, with vanilla and dried fruit'),
  ('Buffalo Trace Kentucky Straight Bourbon', 'nose',   'Vanilla, caramel and mint'),
  ('Buffalo Trace Kentucky Straight Bourbon', 'palate', 'Brown sugar, toffee and oak'),
  ('Buffalo Trace Kentucky Straight Bourbon', 'finish', 'Long, with cinnamon and molasses')
) as v(wname, phase, txt)
join public.whiskies w on w.name = v.wname;
