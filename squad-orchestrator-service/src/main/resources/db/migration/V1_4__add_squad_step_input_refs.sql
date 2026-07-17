alter table squad_step
	add column input_refs jsonb not null default '[]'::jsonb;
