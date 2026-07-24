package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentExecutor;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "squad-orchestration-task-queue")
public class RunAiAgentActivityImpl implements RunAiAgentActivity {

	private static final long TEMPORARY_TEST_DELAY_MILLIS = 3000L;

	private final AgentExecutor agentExecutor;

	@Override
	public SquadStepExecutionResult runAiAgent(SquadStepExecutionRequest request) {
		log.info("Running AI agent. squadId: {}, stepId: {}, stepName: {}, agentKey: {}", request.getSquadId(),
				request.getStepId(), request.getStepName(), request.getAgentKey());
		Map<String, Object> input = resolveAgentInput(request);
		log.info("AI agent input: {}", input);

		waitForStatusTesting();

		Map<String, Object> output = agentExecutor.execute(request.getAgentKey(), request.getStepName(), input);

		log.info("AI agent output: {}", output);

		String message = String.valueOf(output.getOrDefault("message",
				"Executed step \"" + request.getStepName() + "\" using AI agent " + request.getAgentKey()));

		return SquadStepExecutionResult.builder().stepId(request.getStepId()).status("COMPLETED").message(message)
				.output(output).build();
	}

	private Map<String, Object> resolveAgentInput(SquadStepExecutionRequest request) {
		Map<String, Object> input = new LinkedHashMap<>();

		if (request.getInputRefs() == null || request.getInputRefs().isEmpty()) {
			return input;
		}

		for (StepInputRef inputRef : request.getInputRefs()) {
			String targetInput = inputRef.getTargetInput();
			if (targetInput == null || targetInput.isBlank()) {
				throw resolutionFailure(request.getStepId(), inputRef.getFromStepId(), inputRef.getKey(),
						"target-input-missing");
			}

			String fromStepId = inputRef.getFromStepId();
			String key = inputRef.getKey();

			Map<String, Object> fromStepOutput = request.getStepOutputsByStepId().get(fromStepId);
			if (fromStepOutput == null) {
				throw resolutionFailure(request.getStepId(), fromStepId, key, "output-missing");
			}
			if (!fromStepOutput.containsKey(key)) {
				throw resolutionFailure(request.getStepId(), fromStepId, key, "key-missing");
			}

			if (input.containsKey(targetInput)) {
				throw resolutionFailure(request.getStepId(), fromStepId, key, "duplicate-target-input");
			}

			input.put(targetInput, fromStepOutput.get(key));
		}

		return input;
	}

	private ApplicationFailure resolutionFailure(String stepId, String fromStepId, String key, String reason) {
		String message = String.format("stepId=%s fromStepId=%s key=%s reason=%s", stepId, fromStepId, key, reason);
		return ApplicationFailure.newNonRetryableFailure(message, "SQUAD_STEP_INPUT_RESOLUTION_FAILED");
	}

	private void waitForStatusTesting() {
		try {
			Thread.sleep(TEMPORARY_TEST_DELAY_MILLIS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("AI agent execution test delay was interrupted.", exception);
		}
	}
}
