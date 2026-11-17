#!/bin/sh
# Tao tai khoan PostgreSQL rieng cho tung service va chi cho phep tai khoan do ket noi database cua minh.
# Chay duoc nhieu lan (idempotent), dung cho ca volume moi lan volume da co du lieu.
# Bien moi truong: PGHOST, PGUSER, PGPASSWORD (tai khoan quan tri) va <SERVICE>_DB_PASSWORD.
set -e

setup_db() {
  db="$1"; user="$2"; pass="$3"
  if [ -z "$pass" ]; then
    echo "Thieu mat khau cho tai khoan $user" >&2
    exit 1
  fi
  psql -v ON_ERROR_STOP=1 -d postgres -v u="$user" -v p="$pass" -v d="$db" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN', :'u') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'u') \gexec
SELECT format('ALTER ROLE %I LOGIN PASSWORD %L', :'u', :'p') \gexec
SELECT format('CREATE DATABASE %I', :'d') WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'d') \gexec
SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'d') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'d', :'u') \gexec
SELECT format('ALTER DATABASE %I OWNER TO %I', :'d', :'u') \gexec
SQL
  psql -v ON_ERROR_STOP=1 -d "$db" -v u="$user" <<'SQL'
SELECT format('ALTER SCHEMA public OWNER TO %I', :'u') \gexec
SELECT format('ALTER TABLE public.%I OWNER TO %I', tablename, :'u') FROM pg_tables WHERE schemaname = 'public' \gexec
SQL
  echo "OK: $user so huu $db"
}

setup_db auth_db auth_user "$AUTH_DB_PASSWORD"
setup_db tournament_db tournament_user "$TOURNAMENT_DB_PASSWORD"
setup_db team_db team_user "$TEAM_DB_PASSWORD"
setup_db athlete_db athlete_user "$ATHLETE_DB_PASSWORD"
setup_db match_db match_user "$MATCH_DB_PASSWORD"
setup_db result_db result_user "$RESULT_DB_PASSWORD"
setup_db ranking_db ranking_user "$RANKING_DB_PASSWORD"
setup_db notification_db notification_user "$NOTIFICATION_DB_PASSWORD"
