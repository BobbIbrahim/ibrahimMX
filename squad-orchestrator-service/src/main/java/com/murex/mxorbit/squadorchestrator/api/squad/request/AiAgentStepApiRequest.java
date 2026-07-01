package com.murex.mxorbit.squadorchestrator.api.squad.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStepType;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("AI_AGENT")
public class AiAgentStepApiRequest extends SquadStepApiRequest {

	@NotBlank
	private String agentKey;

	@Override
	public SquadStepType getType() {
		return SquadStepType.AI_AGENT;
	}
}
