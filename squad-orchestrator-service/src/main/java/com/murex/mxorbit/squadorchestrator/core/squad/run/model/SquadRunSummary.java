package com.murex.mxorbit.squadorchestrator.core.squad.run.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadRunSummary {

	@NonNull
	private String squadId;

	@NonNull
	private String squadName;

	@NonNull
	private String squadRunId;

	@NonNull
	private Instant startedAt;

	private WorkflowRunStatus overallStatus;

	private Instant completedAt;

	private Long durationMs;
}
