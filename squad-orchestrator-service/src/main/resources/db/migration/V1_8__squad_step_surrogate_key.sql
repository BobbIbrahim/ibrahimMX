-- The client-supplied step id ("s1") was the primary key, so two squads reusing
-- the same step id collided and the later save stole the earlier squad's rows.
alter table squad_step
    add column step_id varchar(255);

update squad_step
set step_id = id
where step_id is null;

alter table squad_step
    alter column step_id set not null;

update squad_step
set id = gen_random_uuid()::text;

alter table squad_step
    add constraint uq_squad_step_squad_id_step_id unique (squad_id, step_id);
