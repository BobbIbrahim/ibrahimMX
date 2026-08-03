package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "squad_edge")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SquadEdgeEntity {

	@Id
	@EqualsAndHashCode.Include
	@Builder.Default
	private String id = UUID.randomUUID().toString();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "squad_id", nullable = false)
	private SquadEntity squad;

	@Column(nullable = false)
	private String sourceStepId;

	@Column(nullable = false)
	private String targetStepId;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SquadEdgeRoutingType routingType = SquadEdgeRoutingType.ALWAYS;

	@Column(name = "condition")
	private String condition;

	@Builder.Default
	@Column(nullable = false)
	private Integer priority = 100;

	@Builder.Default
	@Column(name = "is_default", nullable = false)
	private Boolean isDefault = false;
}
