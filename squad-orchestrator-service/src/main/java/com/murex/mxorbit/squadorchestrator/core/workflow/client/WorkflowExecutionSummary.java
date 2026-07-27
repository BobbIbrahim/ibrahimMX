package com.murex.mxorbit.squadorchestrator.core.workflow.client;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionSummary {

	@NonNull
	private String workflowId;

	@NonNull
	private Instant startTime;

	@NonNull
	private Map<String, String> memo;

	private WorkflowRunStatus status;

	private Instant closeTime;
}
