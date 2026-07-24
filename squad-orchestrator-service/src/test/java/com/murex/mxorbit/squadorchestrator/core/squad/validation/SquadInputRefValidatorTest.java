package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentDefinition;
import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentRegistry;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SquadInputRefValidatorTest {

	@Test
	void shouldValidateValidTargetInput() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "code-sentinel"),
				step("step-2", "Step 2", "test-weaver", ref("step-1", "message", "requirements")),
				edge("step-1", "step-2"))));
	}

	@Test
	void shouldValidateOptionalEmptyInputRefs() {
		assertDoesNotThrow(() -> createValidator().validate(workflow(step("step-1", "Step 1", "code-sentinel"),
				step("step-2", "Step 2", "test-weaver"), edge("step-1", "step-2"))));
	}

	@Test
	void shouldRejectBlankTargetInput() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "code-sentinel"),
				step("step-2", "Step 2", "test-weaver", ref("step-1", "message", " ")), edge("step-1", "step-2")),
				"Step 'Step 2' has an incomplete inputRef.");
	}

	@Test
	void shouldRejectUnknownTargetInput() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "code-sentinel"),
				step("step-2", "Step 2", "test-weaver", ref("step-1", "message", "unknown")), edge("step-1", "step-2")),
				"Step 'Step 2' inputRef target input 'unknown' is not declared by agent 'Test Weaver'.");
	}

	@Test
	void shouldRejectDuplicateTargetInput() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "code-sentinel"), step("step-2", "Step 2", "flow-architect"),
						step("step-3", "Step 3", "test-weaver"),
						step("step-4", "Step 4", "code-sentinel", ref("step-1", "message", "context"),
								ref("step-2", "message", "context")),
						edge("step-1", "step-2"), edge("step-1", "step-3"), edge("step-2", "step-4"),
						edge("step-3", "step-4")),
				"Step 'Step 4' has a duplicate inputRef target input 'context'.");
	}

	@Test
	void shouldRejectDownstreamInputRef() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "code-sentinel"),
						step("step-2", "Step 2", "test-weaver", ref("step-3", "message", "requirements")),
						step("step-3", "Step 3", "flow-architect"), edge("step-1", "step-2"), edge("step-2", "step-3")),
				"Step 'Step 2' inputRef from Step 'Step 3' must reference an upstream ancestor.");
	}

	@Test
	void shouldRejectDuplicateInputRef() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "code-sentinel"),
						step("step-2", "Step 2", "test-weaver", ref("step-1", "message", "requirements"),
								ref("step-1", "message", "requirements")),
						edge("step-1", "step-2")),
				"Step 'Step 2' has a duplicate inputRef from Step 'Step 1' using output key 'message'.");
	}

	@Test
	void shouldRejectUndeclaredSourceOutputKey() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "code-sentinel"),
						step("step-2", "Step 2", "test-weaver", ref("step-1", "details", "requirements")),
						edge("step-1", "step-2")),
				"Step 'Step 2' inputRef from Step 'Step 1' references undeclared output key 'details'.");
	}

	@Test
	void shouldRejectIncompleteInputRef() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "code-sentinel"),
				step("step-2", "Step 2", "test-weaver", incompleteRef("step-1", "message")), edge("step-1", "step-2")),
				"Step 'Step 2' has an incomplete inputRef.");
	}

	@Test
	void shouldRejectWorkflowWithUnknownAgent() {
		assertValidationFailure(workflow(step("step-1", "Step 1", "unknown-agent"),
				step("step-2", "Step 2", "code-sentinel"), edge("step-1", "step-2")),
				"Step 'Step 1' references unknown agent 'unknown-agent'.");
	}

	@Test
	void shouldRejectNoRootWorkflowWhenCycleRemovesAllEntryPoints() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "code-sentinel"), step("step-2", "Step 2", "test-weaver"),
						edge("step-1", "step-2"), edge("step-2", "step-1")),
				"The workflow must contain exactly one root step.");
	}

	@Test
	void shouldRejectNoTerminalWorkflowWhenCycleRemovesAllExits() {
		assertValidationFailure(
				workflow(step("step-1", "Step 1", "code-sentinel"), step("step-2", "Step 2", "test-weaver"),
						edge("step-1", "step-2"), edge("step-2", "step-1")),
				"The workflow must contain exactly one root step.");
	}

	private void assertValidationFailure(CreateSquadRequest request, String expectedMessage) {
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> createValidator().validate(request));
		assertEquals(expectedMessage, exception.getReason());
	}

	private SquadInputRefValidator createValidator() {
		return new SquadInputRefValidator(new TestAgentRegistry());
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

	private static final class TestAgentRegistry implements AgentRegistry {
		private final Map<String, AgentDefinition> agents = Map.of("code-sentinel", AgentDefinition
				.builder().agentKey("code-sentinel").name("Code Sentinel")
				.inputs(List.of("code", "requirements", "context")).outputs(List.of("message", "summary")).build(),
				"test-weaver",
				AgentDefinition.builder().agentKey("test-weaver").name("Test Weaver")
						.inputs(List.of("code", "requirements", "testContext")).outputs(List.of("message")).build(),
				"flow-architect", AgentDefinition.builder().agentKey("flow-architect").name("Flow Architect")
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
