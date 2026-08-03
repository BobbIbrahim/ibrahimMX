package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "squad_routing_decision")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SquadRoutingDecisionEntity {

	@Id
	@EqualsAndHashCode.Include
	private String id;

	@Column(name = "squad_run_id", nullable = false)
	private String squadRunId;

	@Column(name = "squad_id", nullable = false)
	private String squadId;

	@Column(name = "decision_sequence", nullable = false)
	private Integer decisionSequence;

	@Column(name = "source_step_id", nullable = false)
	private String sourceStepId;

	@Column(name = "selected_edge_id")
	private String selectedEdgeId;

	@Column(name = "selected_target_step_id")
	private String selectedTargetStepId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SquadRoutingDecisionOutcome outcome;

	@Column(nullable = false)
	private String reason;
}
