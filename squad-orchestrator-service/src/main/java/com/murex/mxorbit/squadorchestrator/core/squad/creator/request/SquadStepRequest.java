package com.murex.mxorbit.squadorchestrator.core.squad.creator.request;

import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import java.util.ArrayList;
import java.util.List;
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

	@NonNull
	private List<StepInputRef> inputRefs = new ArrayList<>();
}
