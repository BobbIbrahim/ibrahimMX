package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.SquadStepExecutionJpaStore;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "squad-orchestration-task-queue")
public class SaveSquadStepExecutionActivityImpl implements SaveSquadStepExecutionActivity {

	private final SquadStepExecutionJpaStore squadStepExecutionJpaStore;

	@Override
	public void saveSquadStepExecution(SaveSquadStepExecutionRequest request) {
		log.info("Saving squad step execution. squadRunId: {}, stepId: {}, status: {}", request.getSquadRunId(),
				request.getStepId(), request.getStatus());
		squadStepExecutionJpaStore.save(request);
	}
}
