package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.run.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "squad_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SquadRunEntity {

	@Id
	@EqualsAndHashCode.Include
	@Builder.Default
	private String id = UUID.randomUUID().toString();

	@Column(name = "squad_id", nullable = false)
	private String squadId;

	@Column(name = "workflow_id", nullable = false, unique = true)
	private String workflowId;

	@Column(name = "run_id", nullable = false)
	private String runId;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;
}
