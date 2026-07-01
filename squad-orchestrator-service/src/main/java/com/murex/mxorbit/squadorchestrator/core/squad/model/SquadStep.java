package com.murex.mxorbit.squadorchestrator.core.squad.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = AiAgentStep.class, name = "AI_AGENT")
// Add new step types here: @JsonSubTypes.Type(value = HttpCallStep.class, name
// = "etc..")
})
public abstract class SquadStep {

	@NonNull
	private String id;

	@NonNull
	private String name;

	@JsonIgnore
	public abstract SquadStepType getType();
}
