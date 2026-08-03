package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
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
@Table(name = "squad_routing_edge_evaluation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SquadRoutingEdgeEvaluationEntity {

	@Id
	@EqualsAndHashCode.Include
	private String id;

	@Column(name = "routing_decision_id", nullable = false)
	private String routingDecisionId;

	@Column(name = "evaluation_order", nullable = false)
	private Integer evaluationOrder;

	@Column(name = "edge_id", nullable = false)
	private String edgeId;

	@Column(name = "target_step_id", nullable = false)
	private String targetStepId;

	@Enumerated(EnumType.STRING)
	@Column(name = "routing_type", nullable = false)
	private SquadEdgeRoutingType routingType;

	private String condition;

	@Column(nullable = false)
	private Integer priority;

	@Column(name = "is_default", nullable = false)
	private Boolean isDefault;

	@Column(nullable = false)
	private Boolean matched;

	@Column(nullable = false)
	private String reason;
}
