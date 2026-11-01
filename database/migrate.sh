#!/bin/sh
# Executado pelo Job PostgreSQL; as credenciais chegam por Secret, nunca por argumentos.
# Migrações são SQL versionado confiável, sem comandos psql nem controle de transação.
set -eu
LC_ALL=C
export LC_ALL

: "${PGHOST:?PGHOST is required}"
: "${PGPORT:?PGPORT is required}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"
MIGRATIONS_DIR=${MIGRATIONS_DIR:-/migrations}
PGCONNECT_TIMEOUT=${PGCONNECT_TIMEOUT:-10}
PGAPPNAME=oficina-schema-migration
export PGCONNECT_TIMEOUT PGAPPNAME

if [ ! -d "$MIGRATIONS_DIR" ]; then
    echo 'Migration directory does not exist.' >&2
    exit 1
fi

workspace=$(mktemp -d "${TMPDIR:-/tmp}/oficina-migration.XXXXXXXX")
cleanup() {
    rm -f "$workspace"/*.sql
    rmdir "$workspace"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM

versions=
count=0
for source in "$MIGRATIONS_DIR"/*.sql; do
    [ -f "$source" ] || continue
    filename=$(basename "$source")
    case "$filename" in
        *[!A-Za-z0-9_.]*) echo 'Migration filename contains unsupported characters.' >&2; exit 1 ;;
    esac
    if ! printf '%s\n' "$filename" | grep -Eq '^V[0-9]{3}__[a-z0-9_]+\.sql$'; then
        echo 'Invalid migration filename; expected V001__description.sql.' >&2
        exit 1
    fi
    digits=$(printf '%s' "$filename" | cut -c 2-4)
    version=$((1$digits - 1000))
    if [ "$version" -eq 0 ]; then
        echo 'Migration version must be positive.' >&2
        exit 1
    fi
    case ",$versions," in
        *",$version,"*) echo 'Duplicate migration version.' >&2; exit 1 ;;
    esac
    # Copia o conteúdo uma vez: uma atualização do ConfigMap não pode trocar o SQL
    # entre cálculo do checksum e execução desta mesma tentativa.
    cp "$source" "$workspace/$filename"
    if grep -Eq '^[[:space:]]*\\' "$workspace/$filename" ||
       grep -Eiq '(^|;)[[:space:]]*(BEGIN|START[[:space:]]+TRANSACTION|COMMIT|END|ROLLBACK|ABORT)([[:space:];]|$)' "$workspace/$filename"; then
        echo 'Migration must not contain psql commands or explicit transaction control.' >&2
        exit 1
    fi
    versions=${versions:+$versions,}$version
    count=$((count + 1))
done

if [ "$count" -eq 0 ]; then
    echo 'No versioned SQL migrations found.' >&2
    exit 1
fi

plan="$workspace/run.sql"
cat > "$plan" <<'SQL'
\set ON_ERROR_STOP on
\set ON_ERROR_ROLLBACK off
BEGIN;
SET LOCAL lock_timeout = '30s';
SET LOCAL statement_timeout = '120s';
SET LOCAL search_path = pg_catalog, public;
-- Chave estável e específica deste conjunto de migrações, por banco PostgreSQL.
SELECT pg_catalog.pg_advisory_xact_lock(1868982633, 1);

CREATE TABLE IF NOT EXISTS public.oficina_schema_history (
    version INTEGER PRIMARY KEY CHECK (version > 0),
    filename VARCHAR(255) NOT NULL UNIQUE,
    checksum CHAR(64) NOT NULL,
    installed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
SQL

cat >> "$plan" <<SQL
DO \$oficina_guard\$
BEGIN
    IF EXISTS (SELECT 1 FROM public.oficina_schema_history WHERE version NOT IN ($versions)) THEN
        RAISE EXCEPTION 'Migration history contains a version missing from this bundle';
    END IF;
END;
\$oficina_guard\$;
SQL

for migration in "$workspace"/V*.sql; do
    filename=$(basename "$migration")
    digits=$(printf '%s' "$filename" | cut -c 2-4)
    version=$((1$digits - 1000))
    checksum=$(sha256sum "$migration" | cut -d ' ' -f 1)
    if ! printf '%s\n' "$checksum" | grep -Eq '^[a-f0-9]{64}$'; then
        echo 'Could not calculate migration checksum.' >&2
        exit 1
    fi
    cat >> "$plan" <<SQL
SELECT EXISTS (
    SELECT 1 FROM public.oficina_schema_history
    WHERE version = $version AND (filename <> '$filename' OR checksum <> '$checksum')
) AS migration_changed,
NOT EXISTS (SELECT 1 FROM public.oficina_schema_history WHERE version = $version) AS migration_pending
\gset
\if :migration_changed
DO \$oficina_guard\$
BEGIN
    RAISE EXCEPTION 'Migration checksum or filename changed for version $version';
END;
\$oficina_guard\$;
\endif
\if :migration_pending
\echo Applying $filename
SQL
    cat "$migration" >> "$plan"
    cat >> "$plan" <<SQL

INSERT INTO public.oficina_schema_history(version, filename, checksum)
VALUES ($version, '$filename', '$checksum');
\else
\echo Already applied $filename
\endif
SQL
done

printf '\nCOMMIT;\n' >> "$plan"

# Um único processo e uma única transação mantêm o lock até DDL + histórico terem commit.
# -X ignora configurações pessoais psql; -w impede prompt interativo no Job.
psql -X -w --set=ON_ERROR_STOP=on --set=ON_ERROR_ROLLBACK=off \
    --host="$PGHOST" --port="$PGPORT" --dbname="$PGDATABASE" --username="$PGUSER" \
    --file="$plan"
