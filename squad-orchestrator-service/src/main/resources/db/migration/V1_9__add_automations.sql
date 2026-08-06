create table automations
(
    id                   uuid         not null primary key,
    name                 varchar(255) not null,
    assignee_type        varchar(32)  not null,
    assignee_id          varchar(255) not null,
    temporal_schedule_id varchar(255) not null unique,
    frequency            varchar(32)  not null,
    run_time             time,
    weekly_day           smallint,
    every_minutes        integer,
    initial_input        jsonb        not null default '{}',
    created_at           timestamptz  not null,
    updated_at           timestamptz  not null,
    constraint chk_automations_assignee_type check (assignee_type in ('SQUAD', 'AGENT')),
    constraint chk_automations_frequency check (frequency in ('INTERVAL', 'DAILY', 'WEEKDAYS', 'WEEKLY')),
    constraint chk_automations_fields check (
        (frequency = 'INTERVAL' and every_minutes is not null and every_minutes > 0
             and run_time is null and weekly_day is null)
        or (frequency in ('DAILY', 'WEEKDAYS') and run_time is not null
             and weekly_day is null and every_minutes is null)
        or (frequency = 'WEEKLY' and run_time is not null
             and weekly_day between 1 and 7 and every_minutes is null)
    )
);

create index idx_automations_assignee on automations (assignee_type, assignee_id);
