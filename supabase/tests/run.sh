#!/usr/bin/env bash
# Runs the migration, seed and behaviour tests against a throwaway database.
# Usage: PGHOST=... PGPORT=... PGUSER=postgres ./supabase/tests/run.sh
set -euo pipefail
cd "$(dirname "$0")/.."
DB=whisky_test_$$
createdb "$DB"
trap 'dropdb --if-exists "$DB"' EXIT
psql -q -v ON_ERROR_STOP=1 -d "$DB" -f tests/supabase_stub.sql
psql -q -v ON_ERROR_STOP=1 -d "$DB" -f migrations/20261103000000_init.sql
psql -q -v ON_ERROR_STOP=1 -d "$DB" -f seed.sql
psql -q -v ON_ERROR_STOP=1 -d "$DB" -f tests/rls_and_matching.sql
