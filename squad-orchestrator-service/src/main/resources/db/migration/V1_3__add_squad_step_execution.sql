create table squad_step_execution
(
    id           varchar(512) not null primary key,
    squad_run_id varchar(255) not null,
    squad_id     varchar(255) not null,
    step_id      varchar(255) not null,
    step_name    varchar(255) not null,
    status       varchar(64)  not null,
    message      text,
    input        jsonb        not null default '{}',
    output       jsonb        not null default '{}'
);

create index idx_squad_step_execution_run_id on squad_step_execution (squad_run_id);
create index idx_squad_step_execution_squad_id on squad_step_execution (squad_id);
