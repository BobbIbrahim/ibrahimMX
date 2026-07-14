package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
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
@Table(name = "squad_step_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SquadStepExecutionEntity {

	@Id
	@EqualsAndHashCode.Include
	private String id;

	@Column(name = "squad_run_id", nullable = false)
	private String squadRunId;

	@Column(name = "squad_id", nullable = false)
	private String squadId;

	@Column(name = "step_id", nullable = false)
	private String stepId;

	@Column(name = "step_name", nullable = false)
	private String stepName;

	@Column(nullable = false)
	private String status;

	private String message;

	@Builder.Default
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private Map<String, Object> input = new LinkedHashMap<>();

	@Builder.Default
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private Map<String, Object> output = new LinkedHashMap<>();
}
