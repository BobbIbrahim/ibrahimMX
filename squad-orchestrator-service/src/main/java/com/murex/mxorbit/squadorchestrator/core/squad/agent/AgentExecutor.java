package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import java.util.Map;

public interface AgentExecutor {

	Map<String, Object> execute(String agentKey, String stepName, Map<String, Object> input);
}
