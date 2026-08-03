package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentDefinition;
import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentRegistry;
import com.murex.mxorbit.squadorchestrator.core.squad.agent.InMemoryAgentRegistry;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingConditionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SquadInputRefValidatorTest {

	@Test
	void shouldValidateValidTargetInput() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b", ref("step-1", "message", "requirements")),
				edge("step-1", "step-2"))));
	}

	@Test
	void shouldValidateOptionalEmptyInputRefs() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b"), edge("step-1", "step-2"))));
	}

	@Test
	void shouldRejectBlankTargetInput() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b", ref("step-1", "message", " ")), edge("step-1", "step-2")),
				"Step 'Step 2' inputRef must have a targetInput.");
	}

	@Test
	void shouldRejectUnknownTargetInput() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"),
						step("step-2", "Step 2", "validator-agent-b", ref("step-1", "message", "unknown")),
						edge("step-1", "step-2")),
				"Step 'Step 2' inputRef target input 'unknown' is not declared by agent 'Validator Agent B'.");
	}

	@Test
	void shouldRejectDuplicateTargetInput() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-c"), step("step-3", "Step 3", "validator-agent-b"),
				step("step-4", "Step 4", "validator-agent-a", ref("step-1", "message", "context"),
						ref("step-2", "message", "context")),
				whenEdge("step-1", "step-2", "output.route equals FLOW", 10),
				whenEdge("step-1", "step-3", "output.route equals TEST", 20), edge("step-2", "step-4"),
				edge("step-3", "step-4")), "Step 'Step 4' has a duplicate inputRef target input 'context'.");
	}

	@Test
	void shouldRejectDownstreamInputRef() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b", ref("step-3", "message", "requirements")),
				step("step-3", "Step 3", "validator-agent-c"), edge("step-1", "step-2"), edge("step-2", "step-3")),
				"Step 'Step 2' inputRef from Step 'Step 3' must reference an upstream ancestor.");
	}

	@Test
	void shouldRejectDuplicateInputRef() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"),
						step("step-2", "Step 2", "validator-agent-b", ref("step-1", "message", "requirements"),
								ref("step-1", "message", "requirements")),
						edge("step-1", "step-2")),
				"Step 'Step 2' has a duplicate inputRef target input 'requirements'.");
	}

	@Test
	void shouldRejectUndeclaredSourceOutputKey() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"),
						step("step-2", "Step 2", "validator-agent-b", ref("step-1", "details", "requirements")),
						edge("step-1", "step-2")),
				"Step 'Step 2' inputRef from Step 'Step 1' references undeclared output key 'details'.");
	}

	@Test
	void shouldRejectIncompleteInputRef() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b", incompleteRef("step-1", "message")),
				edge("step-1", "step-2")), "Step 'Step 2' inputRef must have a targetInput.");
	}

	@Test
	void shouldRejectWorkflowWithUnknownAgent() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "unknown-agent"),
				step("step-2", "Step 2", "validator-agent-a"), edge("step-1", "step-2")),
				"Step 'Step 1' references unknown agent 'unknown-agent'.");
	}

	@Test
	void shouldRejectNoRootWorkflowWhenCycleRemovesAllEntryPoints() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						edge("step-1", "step-2"), edge("step-2", "step-1")),
				"The workflow must contain exactly one root step.");
	}

	@Test
	void shouldRejectNoTerminalWorkflowWhenCycleRemovesAllExits() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						edge("step-1", "step-2"), edge("step-2", "step-1")),
				"The workflow must contain exactly one root step.");
	}

	@Test
	void shouldValidateChangeClassifierTestSelectorDeploymentPlannerSquad() {
		SquadInputRefValidator validator = new SquadInputRefValidator(
				new InMemoryAgentRegistry("http://localhost:8000"), new SquadRoutingConditionEvaluator());

		assertDoesNotThrow(() -> validator.validate(workflow(step("step-1", "New Step 1", "change-classifier"),
				step("step-2", "New Step 2", "test-selector", ref("step-1", "change", "change"),
						ref("step-1", "changeType", "changeType")),
				step("step-3", "New Step 3", "deployment-planner", ref("step-2", "change", "change"),
						ref("step-2", "changeType", "changeType"), ref("step-2", "test", "test")),
				edge("step-1", "step-2"), edge("step-2", "step-3"))));
	}

	@Test
	void shouldRejectGenuinelyUndeclaredOutputKeyForRealAgentRegistry() {
		SquadInputRefValidator validator = new SquadInputRefValidator(
				new InMemoryAgentRegistry("http://localhost:8000"), new SquadRoutingConditionEvaluator());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> validator.validate(workflow(step("step-1", "New Step 1", "change-classifier"),
						step("step-2", "New Step 2", "test-selector", ref("step-1", "nextAction", "change")),
						edge("step-1", "step-2"))));

		assertEquals("Step 'New Step 2' inputRef from Step 'New Step 1' references undeclared output key 'nextAction'.",
				exception.getReason());
	}

	@Test
	void shouldAcceptLegacyLinearAlwaysEdges() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b"), step("step-3", "Step 3", "validator-agent-c"),
				edge("step-1", "step-2"), edge("step-2", "step-3"))));
	}

	@Test
	void shouldAcceptMultipleWhenEdgesWithDistinctPriorities() {
		assertDoesNotThrow(() -> createValidator()
				.validate(diamondWorkflow(whenEdge("step-1", "step-2", "output.changeType equals BUG_FIX", 10),
						whenEdge("step-1", "step-3", "output.changeType equals ENHANCEMENT", 20))));
	}

	@Test
	void shouldAcceptWhenEdgesWithOneDefaultEdge() {
		assertDoesNotThrow(() -> createValidator()
				.validate(diamondWorkflow(whenEdge("step-1", "step-2", "output.changeType equals BUG_FIX", 10),
						defaultEdge("step-1", "step-3"))));
	}

	@Test
	void shouldAcceptDefaultsUnderDifferentSourceSteps() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b"), step("step-3", "Step 3", "validator-agent-c"),
				defaultEdge("step-1", "step-2"), defaultEdge("step-2", "step-3"))));
	}

	@Test
	void shouldAcceptSameWhenPriorityUnderDifferentSourceSteps() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b"), step("step-3", "Step 3", "validator-agent-c"),
				whenEdge("step-1", "step-2", "output.route equals STEP_2", 10),
				whenEdge("step-2", "step-3", "output.route equals STEP_3", 10))));
	}

	@Test
	void shouldRejectNullRoutingType() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						routingEdge("step-1", "step-2", null, null, 100, false)),
				"Connection from step 'step-1' to step 'step-2' must have a routing type.");
	}

	@Test
	void shouldRejectNullPriority() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						routingEdge("step-1", "step-2", SquadEdgeRoutingType.ALWAYS, null, null, false)),
				"Connection from step 'step-1' to step 'step-2' must have a priority.");
	}

	@Test
	void shouldRejectNegativePriority() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						routingEdge("step-1", "step-2", SquadEdgeRoutingType.ALWAYS, null, -1, false)),
				"Connection from step 'step-1' to step 'step-2' must have a nonnegative priority.");
	}

	@Test
	void shouldRejectWhenEdgeWithNullCondition() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						routingEdge("step-1", "step-2", SquadEdgeRoutingType.WHEN, null, 10, false)),
				"Connection from step 'step-1' to step 'step-2' uses routing type WHEN but has no condition.");
	}

	@Test
	void shouldRejectWhenEdgeWithBlankCondition() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						routingEdge("step-1", "step-2", SquadEdgeRoutingType.WHEN, "   ", 10, false)),
				"Connection from step 'step-1' to step 'step-2' uses routing type WHEN but has no condition.");
	}

	@Test
	void shouldRejectAlwaysEdgeWithCondition() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						routingEdge("step-1", "step-2", SquadEdgeRoutingType.ALWAYS, "output.changeType equals BUG_FIX",
								100, false)),
				"Connection from step 'step-1' to step 'step-2' uses routing type ALWAYS and must not define a condition.");
	}

	@Test
	void shouldRejectWhenEdgeMarkedAsDefault() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						routingEdge("step-1", "step-2", SquadEdgeRoutingType.WHEN, "output.changeType equals BUG_FIX",
								10, true)),
				"Connection from step 'step-1' to step 'step-2' is a default edge and must use routing type ALWAYS.");
	}

	@Test
	void shouldRejectMoreThanOneDefaultForSameSourceStep() {
		assertValidationFailure(diamondWorkflow(defaultEdge("step-1", "step-2"), defaultEdge("step-1", "step-3")),
				"Source step 'step-1' has more than one default outgoing edge.");
	}

	@Test
	void shouldRejectNonDefaultAlwaysEdgeWithOtherOutgoingEdge() {
		assertValidationFailure(
				diamondWorkflow(edge("step-1", "step-2"),
						whenEdge("step-1", "step-3", "output.changeType equals ENHANCEMENT", 20)),
				"Source step 'step-1' has a non-default ALWAYS edge together with other outgoing edges.");
	}

	@Test
	void shouldRejectDuplicateWhenPriorityForSameSourceStep() {
		assertValidationFailure(
				diamondWorkflow(whenEdge("step-1", "step-2", "output.changeType equals BUG_FIX", 10),
						whenEdge("step-1", "step-3", "output.changeType equals ENHANCEMENT", 10)),
				"Source step 'step-1' has more than one WHEN edge with priority 10.");
	}

	@Test
	void shouldAcceptValidSimpleRoutingConditionAtSaveTime() {
		assertDoesNotThrow(() -> createValidator().validate(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						whenEdge("step-1", "step-2", "output.changeType equals BUG_FIX", 10))));
	}

	@Test
	void shouldAcceptValidAndOrRoutingConditionsAtSaveTime() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b"),
				whenEdge("step-1", "step-2", "output.changeType equals BUG_FIX and output.route equals READY", 10))));

		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "validator-agent-a"),
				step("step-2", "Step 2", "validator-agent-b"),
				whenEdge("step-1", "step-2", "output.changeType equals BUG_FIX or output.route equals READY", 10))));
	}

	@Test
	void shouldAcceptValidInAndContainsRoutingConditionsAtSaveTime() {
		assertDoesNotThrow(() -> createValidator().validate(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						whenEdge("step-1", "step-2", "output.changeType in [BUG_FIX, HOTFIX]", 10))));

		assertDoesNotThrow(() -> createValidator().validate(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						whenEdge("step-1", "step-2", "output.changeType contains BUG", 10))));
	}

	@Test
	void shouldRejectMalformedRoutingConditionTextAtSaveTime() {
		assertRoutingConditionFailure("nonsense condition text",
				"Invalid routing condition rule: 'nonsense condition text'.");
	}

	@Test
	void shouldRejectUnsupportedRoutingConditionOperatorAtSaveTime() {
		assertRoutingConditionFailure("output.changeType matches BUG_FIX",
				"Invalid routing condition rule: 'output.changeType matches BUG_FIX'.");
	}

	@Test
	void shouldRejectRoutingConditionFieldWithoutOutputPrefixAtSaveTime() {
		assertRoutingConditionFailure("changeType equals BUG_FIX",
				"Invalid routing condition rule: 'changeType equals BUG_FIX'.");
	}

	@Test
	void shouldRejectMixedAndOrRoutingConditionAtSaveTime() {
		assertRoutingConditionFailure(
				"output.changeType equals BUG_FIX and output.route equals READY or output.test equals UNIT",
				"Routing condition must not mix 'and' and 'or'.");
	}

	@Test
	void shouldRejectParenthesesInRoutingConditionAtSaveTime() {
		assertRoutingConditionFailure("output.changeType equals BUG_FIX and (output.route equals READY)",
				"Routing condition must not contain parentheses.");
	}

	@Test
	void shouldRejectMalformedInRoutingConditionSyntaxAtSaveTime() {
		assertRoutingConditionFailure("output.changeType in BUG_FIX, HOTFIX",
				"Routing condition rule 'output.changeType in BUG_FIX, HOTFIX' must use a bracketed list for operator 'in'.");
	}

	private void assertValidationFailure(CreateSquadRequest request, String expectedMessage) {
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> createValidator().validate(request));
		assertEquals(expectedMessage, exception.getReason());
	}

	private void assertRoutingConditionFailure(String condition, String evaluatorMessage) {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
						whenEdge("step-1", "step-2", condition, 10)),
				"Connection from step 'step-1' to step 'step-2' has an invalid routing condition: " + evaluatorMessage);
	}

	private SquadInputRefValidator createValidator() {
		return new SquadInputRefValidator(new TestAgentRegistry(), new SquadRoutingConditionEvaluator());
	}

	private static CreateSquadRequest workflow(Object... items) {
		List<SquadStepRequest> steps = new ArrayList<>();
		List<SquadEdgeRequest> edges = new ArrayList<>();

		for (Object item : items) {
			if (item instanceof SquadStepRequest step) {
				steps.add(step);
				continue;
			}

			if (item instanceof SquadEdgeRequest edge) {
				edges.add(edge);
				continue;
			}

			throw new IllegalArgumentException("Unsupported workflow item: " + item);
		}

		return CreateSquadRequest.builder().name("Squad").description("Workflow validation test").type("hardcoded-flow")
				.steps(steps).edges(edges).build();
	}

	private static CreateSquadRequest diamondWorkflow(SquadEdgeRequest firstBranch, SquadEdgeRequest secondBranch) {
		return workflow(step("step-1", "Step 1", "validator-agent-a"), step("step-2", "Step 2", "validator-agent-b"),
				step("step-3", "Step 3", "validator-agent-c"), step("step-4", "Step 4", "validator-agent-a"),
				firstBranch, secondBranch, edge("step-2", "step-4"), edge("step-3", "step-4"));
	}

	private static AiAgentStepRequest step(String id, String name, String agentKey, StepInputRef... inputRefs) {
		AiAgentStepRequest step = new AiAgentStepRequest();
		step.setId(id);
		step.setName(name);
		step.setAgentKey(agentKey);
		step.setInputRefs(new ArrayList<>(List.of(inputRefs)));
		return step;
	}

	private static StepInputRef ref(String fromStepId, String key, String targetInput) {
		return StepInputRef.builder().fromStepId(fromStepId).key(key).targetInput(targetInput).build();
	}

	private static StepInputRef incompleteRef(String fromStepId, String key) {
		return StepInputRef.builder().fromStepId(fromStepId).key(key).build();
	}

	private static SquadEdgeRequest edge(String sourceStepId, String targetStepId) {
		return SquadEdgeRequest.builder().sourceStepId(sourceStepId).targetStepId(targetStepId).build();
	}

	private static SquadEdgeRequest whenEdge(String sourceStepId, String targetStepId, String condition,
			Integer priority) {
		return routingEdge(sourceStepId, targetStepId, SquadEdgeRoutingType.WHEN, condition, priority, false);
	}

	private static SquadEdgeRequest defaultEdge(String sourceStepId, String targetStepId) {
		return routingEdge(sourceStepId, targetStepId, SquadEdgeRoutingType.ALWAYS, null, 100, true);
	}

	private static SquadEdgeRequest routingEdge(String sourceStepId, String targetStepId,
			SquadEdgeRoutingType routingType, String condition, Integer priority, boolean isDefault) {
		return SquadEdgeRequest.builder().sourceStepId(sourceStepId).targetStepId(targetStepId).routingType(routingType)
				.condition(condition).priority(priority).isDefault(isDefault).build();
	}

	private static final class TestAgentRegistry implements AgentRegistry {
		private final Map<String, AgentDefinition> agents = Map.of("validator-agent-a", AgentDefinition.builder()
				.agentKey("validator-agent-a").name("Validator Agent A")
				.inputs(List.of("code", "requirements", "context")).outputs(List.of("message", "summary")).build(),
				"validator-agent-b",
				AgentDefinition.builder().agentKey("validator-agent-b").name("Validator Agent B")
						.inputs(List.of("code", "requirements", "testContext", "change", "changeType", "test"))
						.outputs(List.of("message")).build(),
				"validator-agent-c", AgentDefinition.builder().agentKey("validator-agent-c").name("Validator Agent C")
						.inputs(List.of("requirement", "context", "constraints")).outputs(List.of("message")).build());

		@Override
		public Optional<AgentDefinition> findByKey(String agentKey) {
			return Optional.ofNullable(agents.get(agentKey));
		}

		@Override
		public List<AgentDefinition> findAll() {
			return List.copyOf(agents.values());
		}
	}
}
