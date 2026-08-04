package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.SquadRoutingDecisionStore;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.request.SquadRoutingDecisionStoreRequest;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "squad-orchestration-task-queue")
public class SaveSquadRoutingDecisionActivityImpl implements SaveSquadRoutingDecisionActivity {

	private final SquadRoutingDecisionStore squadRoutingDecisionStore;

	@Override
	public void saveSquadRoutingDecision(SquadRoutingDecisionStoreRequest request) {
		log.info("Saving squad routing decision. squadRunId: {}, sourceStepId: {}, sequence: {}, outcome: {}",
				request.getSquadRunId(), request.getDecision().getSourceStepId(), request.getDecisionSequence(),
				request.getDecision().getOutcome());

		squadRoutingDecisionStore.save(request);
	}
}
