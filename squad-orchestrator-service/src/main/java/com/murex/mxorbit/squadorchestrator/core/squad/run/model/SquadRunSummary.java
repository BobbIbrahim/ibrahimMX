package com.murex.mxorbit.squadorchestrator.core.squad.run.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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
}
