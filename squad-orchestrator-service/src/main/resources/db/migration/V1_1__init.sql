create table squad
(
    id          varchar(255) not null primary key,
    name        varchar(255) not null,
    description text,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null
);

create table squad_step
(
    id          varchar(255) not null primary key,
    squad_id    varchar(255) not null references squad (id) on delete cascade,
    type        varchar(255) not null,
    name        varchar(255) not null,
    config      jsonb        not null default '{}'
);

create table squad_edge
(
    id             varchar(255) not null primary key,
    squad_id       varchar(255) not null references squad (id) on delete cascade,
    source_step_id varchar(255) not null,
    target_step_id varchar(255) not null
);

create index idx_squad_step_squad_id on squad_step (squad_id);
create index idx_squad_edge_squad_id on squad_edge (squad_id);
