DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'squad_orchestrator_app') THEN
            CREATE ROLE squad_orchestrator_app LOGIN PASSWORD 'squad_orchestrator_app'
                NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;
        END IF;
    END
$$;

GRANT CONNECT ON DATABASE "squad_orchestrator" TO squad_orchestrator_app;
GRANT CREATE ON DATABASE "squad_orchestrator" TO squad_orchestrator_app;

\connect "squad_orchestrator"

GRANT USAGE, CREATE ON SCHEMA public TO squad_orchestrator_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO squad_orchestrator_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO squad_orchestrator_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO squad_orchestrator_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO squad_orchestrator_app;
