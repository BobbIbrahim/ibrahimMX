package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ActivityImpl(taskQueues = "squad-orchestration-task-queue")
public class RunAiAgentActivityImpl implements RunAiAgentActivity {

	@Override
	public SquadStepExecutionResult runAiAgent(SquadStepExecutionRequest request) {
		log.info("Running AI agent. squadId: {}, stepId: {}, stepName: {}, agentKey: {}", request.getSquadId(),
				request.getStepId(), request.getStepName(), request.getAgentKey());

		return SquadStepExecutionResult.builder().stepId(request.getStepId()).status("COMPLETED")
				.message("Executed step \"" + request.getStepName() + "\" using AI agent " + request.getAgentKey())
				.build();
	}
}
