#!/usr/bin/env bash

set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER test WITH PASSWORD 'changeMe';
    CREATE DATABASE bank_account;
    GRANT ALL PRIVILEGES ON DATABASE bank_account TO test;
    \connect bank_account "$POSTGRES_USER"
    GRANT USAGE, CREATE ON SCHEMA public TO test;
EOSQL