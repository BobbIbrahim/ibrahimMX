package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DummyAgentExecutor implements AgentExecutor {

	@Override
	public Map<String, Object> execute(String agentKey, String stepName, Map<String, Object> input) {
		log.info("Executing dummy agent. agentKey: {}, stepName: {}, input: {}", agentKey, stepName, input);

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("message", "Executed step \"" + stepName + "\" using AI agent " + agentKey);

		return output;
	}
}
