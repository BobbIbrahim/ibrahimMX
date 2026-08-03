create table squad_routing_decision
(
    id                      varchar(512) not null primary key,
    squad_run_id            varchar(255) not null,
    squad_id                varchar(255) not null,
    decision_sequence       integer      not null,
    source_step_id          varchar(255) not null,
    selected_edge_id        varchar(255),
    selected_target_step_id varchar(255),
    outcome                 varchar(64)  not null,
    reason                  text         not null
);

create unique index uq_squad_routing_decision_run_sequence
    on squad_routing_decision (squad_run_id, decision_sequence);

create index idx_squad_routing_decision_run_id
    on squad_routing_decision (squad_run_id);

create index idx_squad_routing_decision_squad_id
    on squad_routing_decision (squad_id);

create table squad_routing_edge_evaluation
(
    id                  varchar(512) not null primary key,
    routing_decision_id varchar(512) not null,
    evaluation_order    integer      not null,
    edge_id             varchar(255) not null,
    target_step_id      varchar(255) not null,
    routing_type        varchar(64)  not null,
    condition           text,
    priority            integer      not null,
    is_default          boolean      not null default false,
    matched             boolean      not null default false,
    reason              text         not null,

    constraint fk_routing_edge_evaluation_decision
        foreign key (routing_decision_id)
            references squad_routing_decision (id)
            on delete cascade
);

create unique index uq_routing_edge_evaluation_decision_order
    on squad_routing_edge_evaluation (routing_decision_id, evaluation_order);

create index idx_routing_edge_evaluation_decision_id
    on squad_routing_edge_evaluation (routing_decision_id);