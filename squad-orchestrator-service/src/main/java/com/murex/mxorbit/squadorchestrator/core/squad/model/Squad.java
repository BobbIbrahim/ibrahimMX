package com.murex.mxorbit.squadorchestrator.core.squad.model;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Squad {

	@NonNull
	private String id;

	@NonNull
	private String name;

	private String description;

	@NonNull
	private List<SquadStep> steps;

	@NonNull
	private List<SquadEdge> edges;

	@NonNull
	private Instant createdAt;

	@NonNull
	private Instant updatedAt;
}
