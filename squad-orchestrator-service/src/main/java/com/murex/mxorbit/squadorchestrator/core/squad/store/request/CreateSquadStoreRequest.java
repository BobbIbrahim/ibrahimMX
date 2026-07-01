package com.murex.mxorbit.squadorchestrator.core.squad.store.request;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
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
public class CreateSquadStoreRequest {

	@NonNull
	private String name;

	private String description;

	private List<SquadStep> steps;

	private List<SquadEdge> edges;
}
