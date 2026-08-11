package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InMemoryAgentRegistry implements AgentRegistry {

    private static final String TICKET_TYPE_CLASSIFIER = "ticket-type-classifier";
    private static final String TEST_SELECTOR = "test-selector";
    private static final String DEPLOYMENT_PLANNER = "deployment-planner";

    private final Map<String, AgentDefinition> agentsByKey = new LinkedHashMap<>();

    public InMemoryAgentRegistry(@Value("${agent-service.base-url}") String agentServiceBaseUrl) {
        if (!StringUtils.hasText(agentServiceBaseUrl)) {
            throw new IllegalArgumentException("agent-service.base-url must not be blank.");
        }

        register(AgentDefinition.builder().agentKey(TICKET_TYPE_CLASSIFIER).name(TICKET_TYPE_CLASSIFIER)
                .serviceUrl(agentServiceBaseUrl).inputs(List.of("change")).outputs(List.of("change", "ticketType"))
                .build());

        register(AgentDefinition.builder().agentKey(TEST_SELECTOR).name(TEST_SELECTOR).serviceUrl(agentServiceBaseUrl)
                .inputs(List.of("change", "ticketType")).outputs(List.of("change", "ticketType", "test")).build());

        register(AgentDefinition.builder().agentKey(DEPLOYMENT_PLANNER).name(DEPLOYMENT_PLANNER)
                .serviceUrl(agentServiceBaseUrl).inputs(List.of("change", "ticketType", "test"))
                .outputs(List.of("change", "ticketType", "test", "nextAction")).build());
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
