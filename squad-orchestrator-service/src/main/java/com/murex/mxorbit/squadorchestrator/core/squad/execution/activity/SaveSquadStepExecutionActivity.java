package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SaveSquadStepExecutionActivity {

	@ActivityMethod
	void saveSquadStepExecution(SaveSquadStepExecutionRequest request);
}
