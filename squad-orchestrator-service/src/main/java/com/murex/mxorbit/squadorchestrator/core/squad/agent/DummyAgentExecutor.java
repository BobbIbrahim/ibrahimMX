package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyAgentExecutor implements AgentExecutor {

	private final AgentRegistry agentRegistry;

	@Override
	public Map<String, Object> execute(String agentKey, String stepName, Map<String, Object> input) {
		log.info("Executing dummy agent. agentKey: {}, stepName: {}, input: {}", agentKey, stepName, input);

		return agentRegistry.findByKey(agentKey).filter(agentDefinition -> !agentDefinition.getOutputs().isEmpty())
				.map(agentDefinition -> {
					Map<String, Object> output = new LinkedHashMap<>();

					for (String outputKey : agentDefinition.getOutputs()) {
						output.put(outputKey, "Executed step \"" + stepName + "\" using AI agent " + agentKey
								+ " [output=" + outputKey + "]");
					}

					return output;
				}).orElseGet(() -> {
					Map<String, Object> output = new LinkedHashMap<>();
					output.put("message", "Executed step \"" + stepName + "\" using AI agent " + agentKey);

					return output;
				});
	}
}
