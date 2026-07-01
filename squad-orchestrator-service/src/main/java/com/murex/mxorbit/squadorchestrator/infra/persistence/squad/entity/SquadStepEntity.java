package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStepType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "squad_step")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SquadStepEntity {

	@Id
	@EqualsAndHashCode.Include
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "squad_id", nullable = false)
	private SquadEntity squad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SquadStepType type;

	@Column(nullable = false)
	private String name;

	/**
	 * Holds type-specific fields for this step (e.g. agentKey for AI_AGENT). Shared
	 * fields are stored as proper columns above.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private Map<String, Object> config;
}
