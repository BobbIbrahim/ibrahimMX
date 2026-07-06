package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.provider.SquadProvider;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "squad-orchestration-task-queue")
public class GetSquadActivityImpl implements GetSquadActivity {

	private final SquadProvider squadProvider;

	@Override
	public Squad getSquadById(String squadId) {
		log.info("Getting squad by id: {}", squadId);

		return squadProvider.getSquadById(squadId).orElseThrow(() -> ApplicationFailure
				.newNonRetryableFailure("Squad not found for id: " + squadId, "SQUAD_NOT_FOUND"));
	}
}
