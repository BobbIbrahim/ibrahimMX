alter table squad_step_execution
	add column started_at timestamptz,
	add column completed_at timestamptz,
	add column duration_ms bigint;
