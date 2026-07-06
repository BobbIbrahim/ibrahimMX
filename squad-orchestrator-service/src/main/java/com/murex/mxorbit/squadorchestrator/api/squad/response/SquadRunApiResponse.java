package com.murex.mxorbit.squadorchestrator.api.squad.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadRunApiResponse {

	@NonNull
	private String squadId;

	@NonNull
	private String workflowId;

	@NonNull
	private String runId;

	@NonNull
	private String status;
}
