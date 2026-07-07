package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSquadResult {

	@NonNull
	private Squad squad;
}
