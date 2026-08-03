package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadRoutingDecisionRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SaveSquadRoutingDecisionActivity {

	@ActivityMethod
	void saveSquadRoutingDecision(SaveSquadRoutingDecisionRequest request);
}
