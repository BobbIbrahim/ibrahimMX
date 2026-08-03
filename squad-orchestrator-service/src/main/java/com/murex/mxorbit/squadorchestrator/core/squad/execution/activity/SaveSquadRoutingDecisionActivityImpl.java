package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadRoutingDecisionRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.SquadRoutingDecisionJpaStore;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "squad-orchestration-task-queue")
public class SaveSquadRoutingDecisionActivityImpl implements SaveSquadRoutingDecisionActivity {

	private final SquadRoutingDecisionJpaStore squadRoutingDecisionJpaStore;

	@Override
	public void saveSquadRoutingDecision(SaveSquadRoutingDecisionRequest request) {
		log.info("Saving squad routing decision. squadRunId: {}, sourceStepId: {}, sequence: {}, outcome: {}",
				request.getSquadRunId(), request.getDecision().getSourceStepId(), request.getDecisionSequence(),
				request.getDecision().getOutcome());

		squadRoutingDecisionJpaStore.save(request);
	}
}
