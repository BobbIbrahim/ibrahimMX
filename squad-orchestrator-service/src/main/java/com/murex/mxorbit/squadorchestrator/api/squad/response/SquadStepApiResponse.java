package com.murex.mxorbit.squadorchestrator.api.squad.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStepType;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = AiAgentStepApiResponse.class, name = "AI_AGENT")
// Add new step types here: @JsonSubTypes.Type(value =
// HttpCallStepApiResponse.class, name = "HTTP_CALL")
})
public abstract class SquadStepApiResponse {

	@NonNull
	private String id;

	@NonNull
	private String name;

	@NonNull
	private List<StepInputRef> inputRefs = new ArrayList<>();

	@JsonIgnore
	public abstract SquadStepType getType();
}
