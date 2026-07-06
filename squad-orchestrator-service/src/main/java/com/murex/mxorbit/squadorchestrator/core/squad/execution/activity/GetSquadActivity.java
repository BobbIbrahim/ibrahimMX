package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface GetSquadActivity {

	@ActivityMethod
	Squad getSquadById(String squadId);
}
