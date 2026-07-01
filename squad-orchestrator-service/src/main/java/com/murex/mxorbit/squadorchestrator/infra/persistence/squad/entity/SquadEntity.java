package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "squad")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SquadEntity {

	@Id
	@EqualsAndHashCode.Include
	@Builder.Default
	private String id = UUID.randomUUID().toString();

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@Builder.Default
	@OneToMany(mappedBy = "squad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<SquadStepEntity> steps = new ArrayList<>();

	@Builder.Default
	@OneToMany(mappedBy = "squad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<SquadEdgeEntity> edges = new ArrayList<>();

	public void addStep(SquadStepEntity step) {
		steps.add(step);
		step.setSquad(this);
	}

	public void addEdge(SquadEdgeEntity edge) {
		edges.add(edge);
		edge.setSquad(this);
	}
}
