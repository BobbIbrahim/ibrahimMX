package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class InMemoryAgentRegistry implements AgentRegistry {

	private final Map<String, AgentDefinition> agentsByKey = new LinkedHashMap<>();

	public InMemoryAgentRegistry() {
		register(AgentDefinition.builder().agentKey("code-sentinel").name("Code Sentinel").inputs(List.of())
				.outputs(List.of("message")).build());

		register(AgentDefinition.builder().agentKey("test-weaver").name("Test Weaver").inputs(List.of())
				.outputs(List.of("message")).build());

		register(AgentDefinition.builder().agentKey("flow-architect").name("Flow Architect").inputs(List.of())
				.outputs(List.of("message")).build());
	}

	@Override
	public Optional<AgentDefinition> findByKey(String agentKey) {
		return Optional.ofNullable(agentsByKey.get(agentKey));
	}

	@Override
	public List<AgentDefinition> findAll() {
		return List.copyOf(agentsByKey.values());
	}

	private void register(AgentDefinition agentDefinition) {
		agentsByKey.put(agentDefinition.getAgentKey(), agentDefinition);
	}
}
