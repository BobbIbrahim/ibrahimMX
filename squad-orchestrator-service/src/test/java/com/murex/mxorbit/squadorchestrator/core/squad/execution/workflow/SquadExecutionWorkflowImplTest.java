package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.GetSquadActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.RunAiAgentActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.SaveSquadStepExecutionActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.SaveSquadRoutingDecisionActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadRoutingDecisionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionOutcome;
import io.temporal.failure.ApplicationFailure;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadExecutionWorkflowImplTest {

	private static final String TASK_QUEUE = "squad-orchestration-task-queue";

	private TestWorkflowEnvironment testEnv;
	private WorkflowClient client;
	private final List<SaveSquadStepExecutionRequest> savedStepExecutions = new CopyOnWriteArrayList<>();
	private final List<SaveSquadRoutingDecisionRequest> savedRoutingDecisions = new CopyOnWriteArrayList<>();

	@BeforeEach
	void setUp() {
		savedStepExecutions.clear();
		savedRoutingDecisions.clear();
		initializeTestEnvironment();
	}

	@AfterEach
	void tearDown() {
		if (testEnv != null) {
			testEnv.close();
		}
	}

	private void initializeTestEnvironment() {
		testEnv = TestWorkflowEnvironment.newInstance();
		client = testEnv.getWorkflowClient();
	}

	private void restartTestEnvironment() {
		if (testEnv != null) {
			testEnv.close();
		}

		savedStepExecutions.clear();
		savedRoutingDecisions.clear();
		initializeTestEnvironment();
	}

	@Test
	void shouldKeepExistingThreeStepLinearExecutionUnchanged() {
		Squad squad = threeStepLinearSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(), Map.of(), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals("COMPLETED", snapshot.result().getStatus());
		assertEquals(WorkflowRunStatus.COMPLETED, snapshot.status().getOverallStatus());
		assertEquals(List.of("step-1", "step-2", "step-3"), fakeRunAiAgentActivity.invokedStepIds());
		assertEquals(3, savedStepExecutions.size());

		SaveSquadStepExecutionRequest step1 = findSavedStep("step-1");
		assertEquals(Map.of("change", "Add retries"), step1.getInput());
		assertEquals("COMPLETED", step1.getStatus());

		SaveSquadStepExecutionRequest step2 = findSavedStep("step-2");
		assertEquals(Map.of("change", "Add retries", "changeType", "step-1-output-changeType"), step2.getInput());

		SaveSquadStepExecutionRequest step3 = findSavedStep("step-3");
		assertTrue(step3.getInput().containsKey("test"));
	}

	@Test
	void shouldExecuteOnlyBugFixBranchForBugFixOutput() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals("COMPLETED", snapshot.result().getStatus());
		assertEquals(List.of("step-1", "step-bug"), fakeRunAiAgentActivity.invokedStepIds());
	}

	@Test
	void shouldExecuteOnlyEnhancementBranchForEnhancementOutput() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "ENHANCEMENT")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		runWorkflowWithStatus(squad.getId());

		assertEquals(List.of("step-1", "step-enh"), fakeRunAiAgentActivity.invokedStepIds());
	}

	@Test
	void shouldSelectDefaultBranchForUnknownOutput() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "UNKNOWN")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		runWorkflowWithStatus(squad.getId());

		assertEquals(List.of("step-1", "step-default"), fakeRunAiAgentActivity.invokedStepIds());
	}

	@Test
	void shouldPreferMatchingWhenEdgeOverDefaultEdge() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		runWorkflowWithStatus(squad.getId());

		assertFalse(fakeRunAiAgentActivity.invokedStepIds().contains("step-default"));
		assertTrue(fakeRunAiAgentActivity.invokedStepIds().contains("step-bug"));
	}

	@Test
	void shouldSelectFirstMatchingWhenEdgeByPriority() {
		Squad squad = priorityRoutingSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		runWorkflowWithStatus(squad.getId());

		assertEquals(List.of("step-1", "step-high-priority"), fakeRunAiAgentActivity.invokedStepIds());
	}

	@Test
	void shouldNeverInvokeUnselectedBranchAgents() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		runWorkflowWithStatus(squad.getId());

		assertFalse(fakeRunAiAgentActivity.invokedStepIds().contains("step-enh"));
		assertFalse(fakeRunAiAgentActivity.invokedStepIds().contains("step-default"));
	}

	@Test
	void shouldNotLeaveUnselectedBranchesPendingAfterCompletion() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals(WorkflowRunStatus.COMPLETED, snapshot.status().getOverallStatus());
		assertEquals(SquadStepExecutionStatus.SKIPPED, statusOf(snapshot.status(), "step-enh"));
		assertEquals(SquadStepExecutionStatus.SKIPPED, statusOf(snapshot.status(), "step-default"));
	}

	@Test
	void shouldPassResolvedInputToSelectedTargetOnly() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		runWorkflowWithStatus(squad.getId());

		SaveSquadStepExecutionRequest selected = findSavedStep("step-bug");
		assertEquals(Map.of("change", "Add retries", "changeType", "BUG_FIX"), selected.getInput());
		assertThrows(AssertionError.class, () -> findSavedStep("step-enh"));
	}

	@Test
	void shouldCompleteWhenSelectedBranchTargetIsTerminal() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "ENHANCEMENT")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals("COMPLETED", snapshot.result().getStatus());
		assertEquals(2, savedStepExecutions.size());
	}

	@Test
	void shouldFailWithExactRoutingMessageWhenNoMatchAndNoDefault() {
		Squad squad = noDefaultConditionalSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "UNKNOWN")), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		WorkflowFailedException exception = assertThrows(WorkflowFailedException.class,
				() -> runWorkflow(squad.getId()));
		ApplicationFailure failure = assertInstanceOf(ApplicationFailure.class, exception.getCause());

		assertEquals("SQUAD_ROUTING_DECISION_FAILED", failure.getType());
		assertTrue(
				failure.getMessage().contains("No routing rule matched for step 'step-1' and no default edge exists."));
		assertEquals(List.of("step-1"), fakeRunAiAgentActivity.invokedStepIds());
	}

	@Test
	void shouldPreventDownstreamExecutionAfterAgentFailure() {
		Squad squad = threeStepLinearSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(
				Map.of("change-classifier", "Boom: root step failed"), Map.of(), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		assertThrows(WorkflowFailedException.class, () -> runWorkflow(squad.getId()));
		assertEquals(List.of("step-1"), fakeRunAiAgentActivity.invokedStepIds());
		assertEquals(1, savedStepExecutions.size());
		assertEquals("FAILED", savedStepExecutions.get(0).getStatus());
	}

	@Test
	void shouldSelectOneRouteAfterEachCompletedStepInMultiStageRouting() {
		Squad squad = multiStageRoutingSquad();
		FakeRunAiAgentActivity fakeRunAiAgentActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX"), "step-bug", output("testsRequired", true)), Map.of());
		startWorker(new FakeGetSquadActivity(squad), fakeRunAiAgentActivity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals(List.of("step-1", "step-bug", "step-bug-tests", "step-final"),
				fakeRunAiAgentActivity.invokedStepIds());
		assertEquals(SquadStepExecutionStatus.SKIPPED, statusOf(snapshot.status(), "step-enh"));
		assertEquals(SquadStepExecutionStatus.SKIPPED, statusOf(snapshot.status(), "step-bug-no-tests"));
	}

	@Test
	void shouldBeIndependentFromInputEdgeOrderWhenRouting() {
		Squad orderedSquad = bugEnhancementDefaultSquad();
		Squad reversedSquad = bugEnhancementDefaultSquadReversedEdges();

		FakeRunAiAgentActivity firstRunActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(orderedSquad), firstRunActivity);
		runWorkflowWithStatus(orderedSquad.getId());
		testEnv.close();
		restartTestEnvironment();

		savedStepExecutions.clear();
		FakeRunAiAgentActivity secondRunActivity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());
		startWorker(new FakeGetSquadActivity(reversedSquad), secondRunActivity);
		runWorkflowWithStatus(reversedSquad.getId());

		assertEquals(List.of("step-1", "step-bug"), firstRunActivity.invokedStepIds());
		assertEquals(List.of("step-1", "step-bug"), secondRunActivity.invokedStepIds());
	}

	private WorkflowRunSnapshot runWorkflowWithStatus(String squadId) {
		SquadExecutionWorkflow workflow = newWorkflowStub();
		SquadExecutionResult result = workflow.execute(
				SquadExecutionRequest.builder().squadId(squadId).initialInput(Map.of("change", "Add retries")).build());
		SquadExecutionStatus status = workflow.getExecutionStatus();
		return new WorkflowRunSnapshot(result, status);
	}

	private SquadExecutionResult runWorkflow(String squadId) {
		SquadExecutionWorkflow workflow = newWorkflowStub();
		return workflow.execute(
				SquadExecutionRequest.builder().squadId(squadId).initialInput(Map.of("change", "Add retries")).build());
	}

	private SquadExecutionWorkflow newWorkflowStub() {
		return client.newWorkflowStub(SquadExecutionWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
	}

	private SaveSquadStepExecutionRequest findSavedStep(String stepId) {
		return savedStepExecutions.stream().filter(saved -> saved.getStepId().equals(stepId)).findFirst()
				.orElseThrow(() -> new AssertionError("No saved execution found for step " + stepId));
	}

	private SquadStepExecutionStatus statusOf(SquadExecutionStatus status, String stepId) {
		return status.getSteps().stream().filter(stepStatus -> stepStatus.getStepId().equals(stepId)).findFirst()
				.orElseThrow(() -> new AssertionError("Missing status for step " + stepId)).getStatus();
	}

	private void startWorker(GetSquadActivity getSquadActivity, FakeRunAiAgentActivity runAiAgentActivity) {
		Worker worker = testEnv.newWorker(TASK_QUEUE);
		worker.registerWorkflowImplementationTypes(SquadExecutionWorkflowImpl.class);
		worker.registerActivitiesImplementations(getSquadActivity, runAiAgentActivity,
				(SaveSquadStepExecutionActivity) request -> savedStepExecutions.add(request),
				(SaveSquadRoutingDecisionActivity) request -> savedRoutingDecisions.add(request));
		testEnv.start();
	}

	private Squad threeStepLinearSquad() {
		SquadStep step1 = AiAgentStep.builder().id("step-1").name("Step 1").agentKey("change-classifier")
				.inputRefs(List.of()).build();
		SquadStep step2 = AiAgentStep.builder().id("step-2").name("Step 2").agentKey("test-selector")
				.inputRefs(List.of(ref("step-1", "change", "change"), ref("step-1", "changeType", "changeType")))
				.build();
		SquadStep step3 = AiAgentStep.builder().id("step-3").name("Step 3").agentKey("deployment-planner")
				.inputRefs(List.of(ref("step-2", "change", "change"), ref("step-2", "changeType", "changeType"),
						ref("step-2", "test", "test")))
				.build();

		return squad("squad-linear", List.of(step1, step2, step3),
				List.of(alwaysEdge("step-1", "step-2"), alwaysEdge("step-2", "step-3")));
	}

	private Squad bugEnhancementDefaultSquad() {
		SquadStep step1 = AiAgentStep.builder().id("step-1").name("Step 1").agentKey("change-classifier")
				.inputRefs(List.of()).build();
		SquadStep stepBug = AiAgentStep.builder().id("step-bug").name("Bug Fix Step").agentKey("bug-fix-agent")
				.inputRefs(List.of(ref("step-1", "change", "change"), ref("step-1", "changeType", "changeType")))
				.build();
		SquadStep stepEnh = AiAgentStep.builder().id("step-enh").name("Enhancement Step").agentKey("enh-agent")
				.inputRefs(List.of(ref("step-1", "change", "change"), ref("step-1", "changeType", "changeType")))
				.build();
		SquadStep stepDefault = AiAgentStep.builder().id("step-default").name("Default Step").agentKey("default-agent")
				.inputRefs(List.of(ref("step-1", "change", "change"), ref("step-1", "changeType", "changeType")))
				.build();

		return squad("squad-conditional", List.of(step1, stepBug, stepEnh, stepDefault),
				List.of(whenEdge("step-1", "step-bug", "output.changeType equals BUG_FIX", 10),
						whenEdge("step-1", "step-enh", "output.changeType equals ENHANCEMENT", 20),
						defaultEdge("step-1", "step-default")));
	}

	private Squad bugEnhancementDefaultSquadReversedEdges() {
		Squad base = bugEnhancementDefaultSquad();
		List<SquadEdge> reversed = new ArrayList<>(base.getEdges());
		java.util.Collections.reverse(reversed);
		return squad("squad-conditional-reversed", base.getSteps(), reversed);
	}

	private Squad noDefaultConditionalSquad() {
		SquadStep step1 = AiAgentStep.builder().id("step-1").name("Step 1").agentKey("change-classifier")
				.inputRefs(List.of()).build();
		SquadStep step2 = AiAgentStep.builder().id("step-2").name("Step 2").agentKey("agent-2")
				.inputRefs(List.of(ref("step-1", "changeType", "changeType"))).build();
		SquadStep step3 = AiAgentStep.builder().id("step-3").name("Step 3").agentKey("agent-3")
				.inputRefs(List.of(ref("step-1", "changeType", "changeType"))).build();

		return squad("squad-no-default", List.of(step1, step2, step3),
				List.of(whenEdge("step-1", "step-2", "output.changeType equals BUG_FIX", 10),
						whenEdge("step-1", "step-3", "output.changeType equals ENHANCEMENT", 20)));
	}

	private Squad priorityRoutingSquad() {
		SquadStep step1 = AiAgentStep.builder().id("step-1").name("Step 1").agentKey("change-classifier")
				.inputRefs(List.of()).build();
		SquadStep high = AiAgentStep.builder().id("step-high-priority").name("High Priority").agentKey("agent-high")
				.inputRefs(List.of(ref("step-1", "changeType", "changeType"))).build();
		SquadStep low = AiAgentStep.builder().id("step-low-priority").name("Low Priority").agentKey("agent-low")
				.inputRefs(List.of(ref("step-1", "changeType", "changeType"))).build();

		return squad("squad-priority", List.of(step1, high, low),
				List.of(whenEdge("step-1", "step-low-priority", "output.changeType in [BUG_FIX, HOTFIX]", 20),
						whenEdge("step-1", "step-high-priority", "output.changeType equals BUG_FIX", 10)));
	}

	private Squad multiStageRoutingSquad() {
		SquadStep step1 = AiAgentStep.builder().id("step-1").name("Classify").agentKey("change-classifier")
				.inputRefs(List.of()).build();
		SquadStep stepBug = AiAgentStep.builder().id("step-bug").name("Bug Branch").agentKey("bug-branch")
				.inputRefs(List.of(ref("step-1", "changeType", "changeType"))).build();
		SquadStep stepEnh = AiAgentStep.builder().id("step-enh").name("Enh Branch").agentKey("enh-branch")
				.inputRefs(List.of(ref("step-1", "changeType", "changeType"))).build();
		SquadStep stepBugTests = AiAgentStep.builder().id("step-bug-tests").name("Bug Tests").agentKey("bug-tests")
				.inputRefs(List.of(ref("step-bug", "testsRequired", "testsRequired"))).build();
		SquadStep stepBugNoTests = AiAgentStep.builder().id("step-bug-no-tests").name("Bug No Tests")
				.agentKey("bug-no-tests").inputRefs(List.of(ref("step-bug", "testsRequired", "testsRequired"))).build();
		SquadStep stepFinal = AiAgentStep.builder().id("step-final").name("Final").agentKey("final-agent")
				.inputRefs(List.of(ref("step-bug-tests", "testsRequired", "testsRequired"))).build();

		return squad("squad-multi-stage", List.of(step1, stepBug, stepEnh, stepBugTests, stepBugNoTests, stepFinal),
				List.of(whenEdge("step-1", "step-bug", "output.changeType equals BUG_FIX", 10),
						defaultEdge("step-1", "step-enh"),
						whenEdge("step-bug", "step-bug-tests", "output.testsRequired equals true", 10),
						defaultEdge("step-bug", "step-bug-no-tests"), alwaysEdge("step-bug-tests", "step-final")));
	}

	private Squad squad(String squadId, List<SquadStep> steps, List<SquadEdge> edges) {
		return Squad.builder().id(squadId).name("Squad").steps(steps).edges(edges)
				.createdAt(Instant.parse("2026-07-23T13:11:50Z")).updatedAt(Instant.parse("2026-07-23T13:11:50Z"))
				.build();
	}

	private static StepInputRef ref(String fromStepId, String key, String targetInput) {
		return StepInputRef.builder().fromStepId(fromStepId).key(key).targetInput(targetInput).build();
	}

	private static SquadEdge alwaysEdge(String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(sourceStepId + "->" + targetStepId).sourceStepId(sourceStepId)
				.targetStepId(targetStepId).routingType(SquadEdgeRoutingType.ALWAYS).condition(null).priority(100)
				.isDefault(false).build();
	}

	private static SquadEdge defaultEdge(String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(sourceStepId + "->" + targetStepId + "::default").sourceStepId(sourceStepId)
				.targetStepId(targetStepId).routingType(SquadEdgeRoutingType.ALWAYS).condition(null).priority(100)
				.isDefault(true).build();
	}

	private static SquadEdge whenEdge(String sourceStepId, String targetStepId, String condition, int priority) {
		return SquadEdge.builder().id(sourceStepId + "->" + targetStepId + "::when::" + priority)
				.sourceStepId(sourceStepId).targetStepId(targetStepId).routingType(SquadEdgeRoutingType.WHEN)
				.condition(condition).priority(priority).isDefault(false).build();
	}

	private static Map<String, Object> output(String key, Object value) {
		Map<String, Object> output = new LinkedHashMap<>();
		output.put(key, value);
		return output;
	}

	private record WorkflowRunSnapshot(SquadExecutionResult result, SquadExecutionStatus status) {
	}

	private record FakeGetSquadActivity(Squad squad) implements GetSquadActivity {
		@Override
		public GetSquadResult getSquad(GetSquadRequest request) {
			return GetSquadResult.builder().squad(squad).build();
		}
	}

	private static final class FakeRunAiAgentActivity implements RunAiAgentActivity {

		private final Map<String, String> failingAgentMessages;
		private final Map<String, Map<String, Object>> outputByStepId;
		private final Map<String, Map<String, Object>> outputByAgentKey;
		private final List<String> invokedStepIds = new CopyOnWriteArrayList<>();

		private FakeRunAiAgentActivity(Map<String, String> failingAgentMessages,
				Map<String, Map<String, Object>> outputByStepId, Map<String, Map<String, Object>> outputByAgentKey) {
			this.failingAgentMessages = failingAgentMessages;
			this.outputByStepId = outputByStepId;
			this.outputByAgentKey = outputByAgentKey;
		}

		@Override
		public SquadStepExecutionResult runAiAgent(SquadStepExecutionRequest request) {
			invokedStepIds.add(request.getStepId());

			String failureMessage = failingAgentMessages.get(request.getAgentKey());
			if (failureMessage != null) {
				throw ApplicationFailure.newNonRetryableFailure(failureMessage, "SQUAD_STEP_AGENT_FAILED");
			}

			Map<String, Object> input = resolveInput(request);
			Map<String, Object> output = new LinkedHashMap<>(input);

			Map<String, Object> byAgent = outputByAgentKey.get(request.getAgentKey());
			if (byAgent != null) {
				output.putAll(byAgent);
			}

			Map<String, Object> byStep = outputByStepId.get(request.getStepId());
			if (byStep != null) {
				output.putAll(byStep);
			}

			output.putIfAbsent("changeType", request.getStepId() + "-output-changeType");
			output.putIfAbsent("test", request.getStepId() + "-output-test");

			return SquadStepExecutionResult.builder().stepId(request.getStepId()).status("COMPLETED")
					.message("Executed " + request.getStepId()).input(input).output(output).build();
		}

		private Map<String, Object> resolveInput(SquadStepExecutionRequest request) {
			Map<String, Object> input = new LinkedHashMap<>();
			if (request.getSeedInput() != null) {
				input.putAll(request.getSeedInput());
			}

			if (request.getInputRefs() == null || request.getInputRefs().isEmpty()) {
				return input;
			}

			for (StepInputRef inputRef : request.getInputRefs()) {
				Map<String, Object> fromStepOutput = request.getStepOutputsByStepId().get(inputRef.getFromStepId());
				input.put(inputRef.getTargetInput(), fromStepOutput.get(inputRef.getKey()));
			}

			return input;
		}

		private List<String> invokedStepIds() {
			return List.copyOf(invokedStepIds);
		}
	}

	@Test
	void shouldExposeAndPersistSuccessfulRoutingDecision() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity activity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX")), Map.of());

		startWorker(new FakeGetSquadActivity(squad), activity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals(1, snapshot.status().getRoutingDecisions().size());
		assertEquals(1, savedRoutingDecisions.size());

		var liveDecision = snapshot.status().getRoutingDecisions().get(0);
		var persistedRequest = savedRoutingDecisions.get(0);

		assertEquals("step-1", liveDecision.getSourceStepId());
		assertEquals("step-bug", liveDecision.getSelectedTargetStepId());
		assertEquals(SquadRoutingDecisionOutcome.CONDITIONAL_MATCH, liveDecision.getOutcome());

		assertEquals(0, persistedRequest.getDecisionSequence());
		assertEquals("step-1", persistedRequest.getDecision().getSourceStepId());
		assertEquals("step-bug", persistedRequest.getDecision().getSelectedTargetStepId());
	}

	@Test
	void shouldPreserveMultipleRoutingDecisionsInExecutionOrder() {
		Squad squad = multiStageRoutingSquad();
		FakeRunAiAgentActivity activity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "BUG_FIX"), "step-bug", output("testsRequired", true)), Map.of());

		startWorker(new FakeGetSquadActivity(squad), activity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals(3, snapshot.status().getRoutingDecisions().size());
		assertEquals(3, savedRoutingDecisions.size());

		assertEquals("step-1", snapshot.status().getRoutingDecisions().get(0).getSourceStepId());
		assertEquals("step-bug", snapshot.status().getRoutingDecisions().get(1).getSourceStepId());
		assertEquals("step-bug-tests", snapshot.status().getRoutingDecisions().get(2).getSourceStepId());

		assertEquals(0, savedRoutingDecisions.get(0).getDecisionSequence());
		assertEquals(1, savedRoutingDecisions.get(1).getDecisionSequence());
		assertEquals(2, savedRoutingDecisions.get(2).getDecisionSequence());
	}

	@Test
	void shouldNotCreateRoutingDecisionForTerminalStep() {
		Squad squad = bugEnhancementDefaultSquad();
		FakeRunAiAgentActivity activity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "ENHANCEMENT")), Map.of());

		startWorker(new FakeGetSquadActivity(squad), activity);

		WorkflowRunSnapshot snapshot = runWorkflowWithStatus(squad.getId());

		assertEquals(1, snapshot.status().getRoutingDecisions().size());
		assertEquals("step-1", snapshot.status().getRoutingDecisions().get(0).getSourceStepId());
		assertEquals(1, savedRoutingDecisions.size());
	}

	@Test
	void shouldPersistNoMatchDecisionBeforeRoutingFailure() {
		Squad squad = noDefaultConditionalSquad();
		FakeRunAiAgentActivity activity = new FakeRunAiAgentActivity(Map.of(),
				Map.of("step-1", output("changeType", "UNKNOWN")), Map.of());

		startWorker(new FakeGetSquadActivity(squad), activity);

		WorkflowFailedException exception = assertThrows(WorkflowFailedException.class,
				() -> runWorkflow(squad.getId()));

		ApplicationFailure failure = assertInstanceOf(ApplicationFailure.class, exception.getCause());

		assertEquals("SQUAD_ROUTING_DECISION_FAILED", failure.getType());
		assertEquals(1, savedRoutingDecisions.size());

		SaveSquadRoutingDecisionRequest saved = savedRoutingDecisions.get(0);

		assertEquals(0, saved.getDecisionSequence());
		assertEquals(SquadRoutingDecisionOutcome.NO_MATCH, saved.getDecision().getOutcome());
		assertEquals("step-1", saved.getDecision().getSourceStepId());
		assertEquals(null, saved.getDecision().getSelectedEdgeId());
		assertEquals(null, saved.getDecision().getSelectedTargetStepId());
		assertEquals(2, saved.getDecision().getCheckedEdges().size());
	}

}
