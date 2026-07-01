package com.murex.mxorbit.squadorchestrator.core.squad.creator.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class SquadStepRequest {

	@NonNull
	private String id;

	@NonNull
	private String name;
}
