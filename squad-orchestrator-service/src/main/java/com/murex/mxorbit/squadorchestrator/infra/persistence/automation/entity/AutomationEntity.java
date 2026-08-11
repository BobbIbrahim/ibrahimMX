package com.murex.mxorbit.squadorchestrator.infra.persistence.automation.entity;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "automations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AutomationEntity {

	@Id
	@EqualsAndHashCode.Include
	@Builder.Default
	private UUID id = UUID.randomUUID();

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "assignee_type", nullable = false)
	private AssigneeType assigneeType;

	@Column(name = "assignee_id", nullable = false)
	private String assigneeId;

	@Column(name = "temporal_schedule_id", nullable = false, unique = true)
	private String temporalScheduleId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AutomationFrequency frequency;

	@JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
	@Column(name = "run_time")
	private OffsetDateTime runTime;

	@Column(name = "weekly_day")
	private Integer weeklyDay;

	@Column(name = "every_minutes")
	private Integer everyMinutes;

	@Builder.Default
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "initial_input", nullable = false)
	private Map<String, Object> initialInput = new LinkedHashMap<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
