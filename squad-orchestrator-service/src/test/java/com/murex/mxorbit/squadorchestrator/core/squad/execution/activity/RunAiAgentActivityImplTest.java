package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentExecutor;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import io.temporal.failure.ApplicationFailure;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunAiAgentActivityImplTest {

	@Test
	void shouldResolveOneTargetInput() {
		CapturingAgentExecutor executor = new CapturingAgentExecutor();
		RunAiAgentActivityImpl activity = new RunAiAgentActivityImpl(executor);

		SquadStepExecutionResult result = activity.runAiAgent(
				request(ref("step-1", "message", "requirements"), Map.of("step-1", Map.of("message", "value"))));

		assertEquals(Map.of("requirements", "value"), executor.lastInput);
		assertEquals("COMPLETED", result.getStatus());
	}

	@Test
	void shouldResolveMultipleTargetInputs() {
		CapturingAgentExecutor executor = new CapturingAgentExecutor();
		RunAiAgentActivityImpl activity = new RunAiAgentActivityImpl(executor);

		activity.runAiAgent(
				request(List.of(ref("step-1", "message", "requirements"), ref("step-2", "summary", "context")),
						Map.of("step-1", Map.of("message", "value-1"), "step-2", Map.of("summary", "value-2"))));

		assertEquals(Map.of("requirements", "value-1", "context", "value-2"), executor.lastInput);
	}

	@Test
	void shouldReturnEmptyInputForEmptyInputRefs() {
		CapturingAgentExecutor executor = new CapturingAgentExecutor();
		RunAiAgentActivityImpl activity = new RunAiAgentActivityImpl(executor);

		activity.runAiAgent(request(List.of(), Map.of()));

		assertTrue(executor.lastInput.isEmpty());
	}

	@Test
	void shouldPreserveMissingSourceOutputFailure() {
		RunAiAgentActivityImpl activity = new RunAiAgentActivityImpl(new CapturingAgentExecutor());

		ApplicationFailure exception = assertThrows(ApplicationFailure.class,
				() -> activity.runAiAgent(request(List.of(ref("step-1", "message", "requirements")), Map.of())));

		assertTrue(exception.getMessage().contains("output-missing"));
	}

	@Test
	void shouldPreserveMissingSourceOutputKeyFailure() {
		RunAiAgentActivityImpl activity = new RunAiAgentActivityImpl(new CapturingAgentExecutor());

		ApplicationFailure exception = assertThrows(ApplicationFailure.class,
				() -> activity.runAiAgent(request(List.of(ref("step-1", "message", "requirements")),
						Map.of("step-1", Map.of("other", "value")))));

		assertTrue(exception.getMessage().contains("key-missing"));
	}

	@Test
	void shouldRejectBlankTargetInputAtRuntime() {
		RunAiAgentActivityImpl activity = new RunAiAgentActivityImpl(new CapturingAgentExecutor());

		ApplicationFailure exception = assertThrows(ApplicationFailure.class, () -> activity.runAiAgent(
				request(List.of(ref("step-1", "message", " ")), Map.of("step-1", Map.of("message", "value")))));

		assertTrue(exception.getMessage().contains("target-input-missing"));
	}

	@Test
	void shouldRejectDuplicateTargetInputAtRuntime() {
		RunAiAgentActivityImpl activity = new RunAiAgentActivityImpl(new CapturingAgentExecutor());

		ApplicationFailure exception = assertThrows(ApplicationFailure.class,
				() -> activity.runAiAgent(request(
						List.of(ref("step-1", "message", "requirements"), ref("step-2", "summary", "requirements")),
						Map.of("step-1", Map.of("message", "value-1"), "step-2", Map.of("summary", "value-2")))));

		assertTrue(exception.getMessage().contains("duplicate-target-input"));
	}

	private static SquadStepExecutionRequest request(StepInputRef inputRef, Map<String, Map<String, Object>> outputs) {
		return request(List.of(inputRef), outputs);
	}

	private static SquadStepExecutionRequest request(List<StepInputRef> inputRefs,
			Map<String, Map<String, Object>> outputs) {
		return SquadStepExecutionRequest.builder().squadId("squad-1").stepId("step-2").stepName("Step 2")
				.agentKey("test-weaver").inputRefs(inputRefs).stepOutputsByStepId(new LinkedHashMap<>(outputs)).build();
	}

	private static StepInputRef ref(String fromStepId, String key, String targetInput) {
		return StepInputRef.builder().fromStepId(fromStepId).key(key).targetInput(targetInput).build();
	}

	private static final class CapturingAgentExecutor implements AgentExecutor {
		private Map<String, Object> lastInput = Map.of();

		@Override
		public Map<String, Object> execute(String agentKey, String stepName, Map<String, Object> input) {
			lastInput = new LinkedHashMap<>(input);
			return Map.of("message", "ok");
		}
	}
}
