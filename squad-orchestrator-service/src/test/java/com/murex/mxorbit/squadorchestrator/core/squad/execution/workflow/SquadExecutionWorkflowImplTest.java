package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.GetSquadActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.RunAiAgentActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.SaveSquadStepExecutionActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadExecutionWorkflowImplTest {

	private static final String TASK_QUEUE = "squad-orchestration-task-queue";

	private TestWorkflowEnvironment testEnv;
	private WorkflowClient client;
	private final List<SaveSquadStepExecutionRequest> savedStepExecutions = new CopyOnWriteArrayList<>();

	@BeforeEach
	void setUp() {
		testEnv = TestWorkflowEnvironment.newInstance();
		client = testEnv.getWorkflowClient();
	}

	@AfterEach
	void tearDown() {
		testEnv.close();
	}

	@Test
	void shouldPersistResolvedInputOutputAndCompletedStatusForSuccessfulSteps() {
		Squad squad = threeStepSquad();
		startWorker(new FakeGetSquadActivity(squad), new FakeRunAiAgentActivity(Map.of()));

		SquadExecutionResult result = runWorkflow(squad.getId());

		assertEquals("COMPLETED", result.getStatus());
		assertEquals(3, savedStepExecutions.size());

		SaveSquadStepExecutionRequest step1 = findSavedStep("step-1");
		assertEquals("COMPLETED", step1.getStatus());
		assertEquals(Map.of("change", "Add retries"), step1.getInput());
		assertEquals(
				Map.of("change", "Add retries", "changeType", "step-1-output-changeType", "test", "step-1-output-test"),
				step1.getOutput());

		SaveSquadStepExecutionRequest step2 = findSavedStep("step-2");
		assertEquals("COMPLETED", step2.getStatus());
		assertEquals(Map.of("change", "Add retries", "changeType", "step-1-output-changeType"), step2.getInput());

		SaveSquadStepExecutionRequest step3 = findSavedStep("step-3");
		assertEquals("COMPLETED", step3.getStatus());
		assertTrue(step3.getInput().containsKey("test"));
	}

	@Test
	void shouldReachFailedStatusAndPreserveErrorMessageForFailedStep() {
		Squad squad = twoStepSquad();
		Map<String, String> failingAgents = Map.of("test-selector", "Boom: agent unavailable");
		startWorker(new FakeGetSquadActivity(squad), new FakeRunAiAgentActivity(failingAgents));

		WorkflowFailedException exception = assertThrows(WorkflowFailedException.class,
				() -> runWorkflow(squad.getId()));
		assertTrue(exception.getCause().getMessage().contains("Boom: agent unavailable"));

		SaveSquadStepExecutionRequest step2 = findSavedStep("step-2");
		assertEquals("FAILED", step2.getStatus());
		assertTrue(step2.getMessage().contains("Boom: agent unavailable"));
		assertTrue(String.valueOf(step2.getOutput().get("error")).contains("Boom: agent unavailable"));
		assertEquals(Map.of("change", "Add retries"), step2.getInput());
	}

	@Test
	void shouldNotExecuteDependentStepsAfterAFailure() {
		Squad squad = threeStepSquad();
		Map<String, String> failingAgents = Map.of("change-classifier", "Boom: root step failed");
		startWorker(new FakeGetSquadActivity(squad), new FakeRunAiAgentActivity(failingAgents));

		assertThrows(WorkflowFailedException.class, () -> runWorkflow(squad.getId()));

		assertEquals(1, savedStepExecutions.size());
		assertEquals("step-1", savedStepExecutions.get(0).getStepId());
		assertEquals("FAILED", savedStepExecutions.get(0).getStatus());
	}

	@Test
	void shouldPreserveEachStepInputAndOutputForFullThreeStepExecution() {
		Squad squad = threeStepSquad();
		startWorker(new FakeGetSquadActivity(squad), new FakeRunAiAgentActivity(Map.of()));

		runWorkflow(squad.getId());

		assertEquals(3, savedStepExecutions.size());
		assertFalse(findSavedStep("step-1").getOutput().isEmpty());
		assertFalse(findSavedStep("step-2").getOutput().isEmpty());
		assertFalse(findSavedStep("step-3").getOutput().isEmpty());

		SaveSquadStepExecutionRequest terminalStep = findSavedStep("step-3");
		assertEquals("COMPLETED", terminalStep.getStatus());
	}

	private SquadExecutionResult runWorkflow(String squadId) {
		SquadExecutionWorkflow workflow = client.newWorkflowStub(SquadExecutionWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

		return workflow.execute(
				SquadExecutionRequest.builder().squadId(squadId).initialInput(Map.of("change", "Add retries")).build());
	}

	private SaveSquadStepExecutionRequest findSavedStep(String stepId) {
		return savedStepExecutions.stream().filter(saved -> saved.getStepId().equals(stepId)).findFirst()
				.orElseThrow(() -> new AssertionError("No saved execution found for step " + stepId));
	}

	private void startWorker(GetSquadActivity getSquadActivity, RunAiAgentActivity runAiAgentActivity) {
		Worker worker = testEnv.newWorker(TASK_QUEUE);
		worker.registerWorkflowImplementationTypes(SquadExecutionWorkflowImpl.class);
		worker.registerActivitiesImplementations(getSquadActivity, runAiAgentActivity,
				(SaveSquadStepExecutionActivity) request -> savedStepExecutions.add(request));
		testEnv.start();
	}

	private Squad threeStepSquad() {
		SquadStep step1 = AiAgentStep.builder().id("step-1").name("Step 1").agentKey("change-classifier")
				.inputRefs(List.of()).build();
		SquadStep step2 = AiAgentStep.builder().id("step-2").name("Step 2").agentKey("test-selector")
				.inputRefs(List.of(ref("step-1", "change", "change"), ref("step-1", "changeType", "changeType")))
				.build();
		SquadStep step3 = AiAgentStep.builder().id("step-3").name("Step 3").agentKey("deployment-planner")
				.inputRefs(List.of(ref("step-2", "change", "change"), ref("step-2", "changeType", "changeType"),
						ref("step-2", "test", "test")))
				.build();

		return squad(List.of(step1, step2, step3), List.of(edge("step-1", "step-2"), edge("step-2", "step-3")));
	}

	private Squad twoStepSquad() {
		SquadStep step1 = AiAgentStep.builder().id("step-1").name("Step 1").agentKey("change-classifier")
				.inputRefs(List.of()).build();
		SquadStep step2 = AiAgentStep.builder().id("step-2").name("Step 2").agentKey("test-selector")
				.inputRefs(List.of(ref("step-1", "change", "change"))).build();

		return squad(List.of(step1, step2), List.of(edge("step-1", "step-2")));
	}

	private Squad squad(List<SquadStep> steps, List<SquadEdge> edges) {
		return Squad.builder().id("squad-1").name("Squad").steps(steps).edges(edges)
				.createdAt(Instant.parse("2026-07-23T13:11:50Z")).updatedAt(Instant.parse("2026-07-23T13:11:50Z"))
				.build();
	}

	private static StepInputRef ref(String fromStepId, String key, String targetInput) {
		return StepInputRef.builder().fromStepId(fromStepId).key(key).targetInput(targetInput).build();
	}

	private static SquadEdge edge(String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(sourceStepId + "->" + targetStepId).sourceStepId(sourceStepId)
				.targetStepId(targetStepId).build();
	}

	private record FakeGetSquadActivity(Squad squad) implements GetSquadActivity {
		@Override
		public GetSquadResult getSquad(GetSquadRequest request) {
			return GetSquadResult.builder().squad(squad).build();
		}
	}

	/**
	 * Fake agent activity resolving inputs the same way the real activity does, but
	 * without network/agent-registry dependencies, so workflow orchestration can be
	 * tested in isolation. Agent keys present in {@code failingAgentMessages} throw
	 * a failure instead of succeeding.
	 */
	private record FakeRunAiAgentActivity(Map<String, String> failingAgentMessages) implements RunAiAgentActivity {
		@Override
		public SquadStepExecutionResult runAiAgent(SquadStepExecutionRequest request) {
			String failureMessage = failingAgentMessages.get(request.getAgentKey());
			if (failureMessage != null) {
				throw io.temporal.failure.ApplicationFailure.newNonRetryableFailure(failureMessage,
						"SQUAD_STEP_AGENT_FAILED");
			}

			Map<String, Object> input = new LinkedHashMap<>();
			if (request.getInputRefs().isEmpty()) {
				input.putAll(request.getSeedInput());
			} else {
				for (StepInputRef inputRef : request.getInputRefs()) {
					Map<String, Object> fromStepOutput = request.getStepOutputsByStepId().get(inputRef.getFromStepId());
					input.put(inputRef.getTargetInput(), fromStepOutput.get(inputRef.getKey()));
				}
			}

			Map<String, Object> output = new LinkedHashMap<>(input);
			output.put("changeType", request.getStepId() + "-output-changeType");
			output.put("test", request.getStepId() + "-output-test");

			return SquadStepExecutionResult.builder().stepId(request.getStepId()).status("COMPLETED")
					.message("Executed " + request.getStepId()).input(input).output(output).build();
		}
	}
}
