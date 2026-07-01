package com.murex.mxorbit.squadorchestrator.api.squad.response;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStepType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("AI_AGENT")
public class AiAgentStepApiResponse extends SquadStepApiResponse {

	@NonNull
	private String agentKey;

	@Override
	public SquadStepType getType() {
		return SquadStepType.AI_AGENT;
	}
}
