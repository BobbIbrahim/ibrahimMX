package com.murex.mxorbit.squadorchestrator.core.squad.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("AI_AGENT")
public class AiAgentStep extends SquadStep {

	@NonNull
	private String agentKey;

	@Override
	public SquadStepType getType() {
		return SquadStepType.AI_AGENT;
	}
}
