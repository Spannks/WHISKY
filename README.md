# Whisky Journal

Android app for recording whisky tastings: what you tasted, which flavours you actually found, when and
where, the event you were at and the brand person you spoke to — building up a picture of what you like.

This repo holds **MVP slices 1–3** from the design doc:

1. Google sign-in (Credential Manager → Supabase Auth), email magic-link fallback, age gate, settings, account deletion
2. Whisky search against a seeded catalogue (+ "add it" for bottles that aren't listed), tasting record with when / where / event / brand contact / rating / would-buy / notes
3. Flavour confirmation: catalogue tasting notes are fuzzy-matched to a flavour vocabulary and shown as chips you confirm, reject or mark unsure, with strength and free-search to add more

Next slices: camera label scan, flavour-profile chart (the `user_flavour_profile` view already computes it), retailer links.

```
android/     Kotlin + Jetpack Compose app
supabase/
  migrations/   schema, RLS, fuzzy matching, RPCs
  seed.sql      flavour vocabulary + 20 sample whiskies
  functions/    delete-account Edge Function
  tests/        runs the schema against plain Postgres and checks RLS + matching
```

## Setup

### 1. Supabase

1. Create a project at [supabase.com](https://supabase.com) (choose the London region).
2. **SQL editor** → run `supabase/migrations/20261103000000_init.sql`, then `supabase/seed.sql`.
   (Or with the CLI: `supabase link --project-ref <ref>` then `supabase db push` and run the seed.)
3. Deploy the deletion function: `supabase functions deploy delete-account`.
4. **Auth → URL configuration** → add `whiskyjournal://login-callback` to Redirect URLs (for the email link).

### 2. Google sign-in

1. In [Google Cloud Console](https://console.cloud.google.com/apis/credentials) create an OAuth consent screen, then two OAuth clients:
   - **Android** — package `app.whiskyjournal`, plus the SHA-1 of your debug keystore
     (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`).
   - **Web application** — this ID is the one the app and Supabase use.
2. Supabase → **Auth → Providers → Google** → enable, paste the Web client ID and secret.
   Under *Authorized Client IDs* add the Web client ID.

### 3. Run the app

1. Copy `android/local.properties.example` to `android/local.properties` and fill in the three values.
2. Open the `android/` folder in Android Studio, let Gradle sync, run on a device or emulator with Google Play.

Android Studio may offer newer library versions — they're pinned in `android/gradle/libs.versions.toml`.

## Tests

`./supabase/tests/run.sh` (needs a local Postgres 15+ with `PGHOST`/`PGUSER` set) creates a throwaway database, applies a
small Supabase stand-in, the migration and seed, then checks: profile auto-creation, fuzzy matching (including
misspellings), search, the age gate, saving a tasting, the flavour-profile scores, that one user can't see another's
data, and that deleting an account cascades. CI runs these and builds a debug APK on every push.

## Notes and decisions

- **Sample catalogue.** The 20 whiskies and their tasting notes in `seed.sql` are short descriptors written for
  testing (`source = 'curated-sample'`); ABVs vary by market. Replace with a curated or licensed catalogue before launch.
- **Flavour matching** runs in Postgres when a tasting note is added: whole-word synonym matches score 1.0;
  remaining phrases are trigram-matched for misspellings. Tune via `flavour_synonyms` and the `match_flavours` threshold.
- **Profile scoring** (in `user_flavour_profile`): a confirmed flavour counts +2 in a 5★ dram down to −1 in a 1★ dram,
  scaled by strength; a rejected flavour counts −0.5.
- **Privacy.** Brand contacts and events are private to the user (RLS). Deleting the account removes everything.
- **Not yet built:** offline-first storage (tastings currently need signal), location capture (columns exist),
  encrypted session storage, Play Store web deletion page.
