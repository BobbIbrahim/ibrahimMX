alter table squad_edge
	add column routing_type varchar(16) not null default 'ALWAYS',
	add column condition text,
	add column priority integer not null default 100,
	add column is_default boolean not null default false;
