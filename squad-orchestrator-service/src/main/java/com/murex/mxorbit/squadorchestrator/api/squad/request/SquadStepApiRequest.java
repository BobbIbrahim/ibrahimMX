package com.murex.mxorbit.squadorchestrator.api.squad.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStepType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = AiAgentStepApiRequest.class, name = "AI_AGENT")
// Add new step types here: @JsonSubTypes.Type(value =
// HttpCallStepApiRequest.class, name = "HTTP_CALL")
})
public abstract class SquadStepApiRequest {

	@NotBlank
	private String id;

	@NotBlank
	private String name;

	@JsonIgnore
	public abstract SquadStepType getType();
}
