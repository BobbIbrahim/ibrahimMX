package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryAgentRegistryTest {

	@Test
	void shouldDeclareChangeClassifierOutputsIncludingChange() {
		AgentDefinition agentDefinition = findAgent("change-classifier");

		assertEquals(List.of("change"), agentDefinition.getInputs());
		assertEquals(List.of("change", "changeType"), agentDefinition.getOutputs());
	}

	@Test
	void shouldDeclareTestSelectorOutputsIncludingChangeAndChangeType() {
		AgentDefinition agentDefinition = findAgent("test-selector");

		assertEquals(List.of("change", "changeType"), agentDefinition.getInputs());
		assertEquals(List.of("change", "changeType", "test"), agentDefinition.getOutputs());
	}

	@Test
	void shouldDeclareDeploymentPlannerOutputsIncludingUpstreamKeys() {
		AgentDefinition agentDefinition = findAgent("deployment-planner");

		assertEquals(List.of("change", "changeType", "test"), agentDefinition.getInputs());
		assertEquals(List.of("change", "changeType", "test", "nextAction"), agentDefinition.getOutputs());
	}

	@Test
	void shouldExposeAllRegisteredAgents() {
		InMemoryAgentRegistry registry = new InMemoryAgentRegistry("http://localhost:8000");

		assertTrue(registry.findAll().stream().map(AgentDefinition::getAgentKey).toList()
				.containsAll(List.of("change-classifier", "test-selector", "deployment-planner")));
	}

	private static AgentDefinition findAgent(String agentKey) {
		InMemoryAgentRegistry registry = new InMemoryAgentRegistry("http://localhost:8000");
		Optional<AgentDefinition> agentDefinition = registry.findByKey(agentKey);
		assertTrue(agentDefinition.isPresent(), "Expected agent '" + agentKey + "' to be registered.");
		return agentDefinition.get();
	}
}
