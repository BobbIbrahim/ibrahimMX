package com.murex.mxorbit.squadorchestrator.core.squad.creator.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiAgentStepRequest extends SquadStepRequest {

	@NonNull
	private String agentKey;
}
