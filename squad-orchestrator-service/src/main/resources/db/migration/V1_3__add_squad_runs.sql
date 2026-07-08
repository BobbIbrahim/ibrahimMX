create table squad_runs
(
    id          varchar(255) not null primary key,
    squad_id    varchar(255) not null references squad (id) on delete cascade,
    workflow_id varchar(255) not null unique,
    run_id      varchar(255) not null,
    started_at  timestamptz  not null
);

create index idx_squad_runs_squad_id on squad_runs (squad_id);
