package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {

	Optional<AgentDefinition> findByKey(String agentKey);

	List<AgentDefinition> findAll();
}
