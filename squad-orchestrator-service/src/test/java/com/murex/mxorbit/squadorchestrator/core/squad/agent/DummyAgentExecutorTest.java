package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DummyAgentExecutorTest {

	@Test
	void shouldEmitDeclaredOutputsForKnownAgent() {
		DummyAgentExecutor executor = new DummyAgentExecutor(new TestAgentRegistry());

		Map<String, Object> output = executor.execute("code-sentinel", "Step 1", Map.of("code", "value"));

		assertEquals(List.of("message", "review", "summary"), new ArrayList<>(output.keySet()));
		assertTrue(output.values().stream().allMatch(value -> value instanceof String && !((String) value).isBlank()));
	}

	@Test
	void shouldFallbackToSingleMessageForUnknownAgent() {
		DummyAgentExecutor executor = new DummyAgentExecutor(new TestAgentRegistry());

		Map<String, Object> output = executor.execute("missing-agent", "Step 1", Map.of());

		assertEquals(List.of("message"), new ArrayList<>(output.keySet()));
		assertFalse(((String) output.get("message")).isBlank());
	}

	private static final class TestAgentRegistry implements AgentRegistry {

		@Override
		public Optional<AgentDefinition> findByKey(String agentKey) {
			if (!"code-sentinel".equals(agentKey)) {
				return Optional.empty();
			}

			return Optional.of(AgentDefinition.builder().agentKey("code-sentinel").name("Code Sentinel")
					.inputs(List.of("code", "requirements", "context")).outputs(List.of("message", "review", "summary"))
					.build());
		}

		@Override
		public List<AgentDefinition> findAll() {
			return List.of(AgentDefinition.builder().agentKey("code-sentinel").name("Code Sentinel")
					.inputs(List.of("code", "requirements", "context")).outputs(List.of("message", "review", "summary"))
					.build());
		}
	}
}
