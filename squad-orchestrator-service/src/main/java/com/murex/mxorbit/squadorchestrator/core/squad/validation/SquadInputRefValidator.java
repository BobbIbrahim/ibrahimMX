package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentRegistry;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SquadInputRefValidator {

    private final AgentRegistry agentRegistry;

    public void validate(CreateSquadRequest request) {
        Map<String, SquadStepRequest> stepMap = buildStepMap(request.getSteps());
        Map<String, Set<String>> reverseEdges = buildReverseEdges(request.getEdges());

        for (SquadStepRequest step : request.getSteps()) {
            Set<String> seenKeys = new HashSet<>();
            for (StepInputRef ref : step.getInputRefs()) {
                validateRef(step.getId(), ref, stepMap, reverseEdges, seenKeys);
            }
        }
    }

    private void validateRef(String stepId, StepInputRef ref, Map<String, SquadStepRequest> stepMap,
                             Map<String, Set<String>> reverseEdges, Set<String> seenKeys) {
        String fromStepId = ref.getFromStepId();
        String key = ref.getKey();

        if (!stepMap.containsKey(fromStepId)) {
            reject(stepId, fromStepId, key, "unknown-step");
        }

        Set<String> ancestors = computeAncestors(stepId, reverseEdges);
        if (!ancestors.contains(fromStepId)) {
            reject(stepId, fromStepId, key, "not-ancestor");
        }

        validateProduces(stepId, fromStepId, key, stepMap);

        if (!seenKeys.add(key)) {
            reject(stepId, fromStepId, key, "duplicate");
        }
    }

    private void validateProduces(String stepId, String fromStepId, String key, Map<String, SquadStepRequest> stepMap) {
        SquadStepRequest fromStep = stepMap.get(fromStepId);
        if (!(fromStep instanceof AiAgentStepRequest aiStep)) {
            throw buildException(stepId, fromStepId, key, "not-produced");
        }
        List<String> outputs = agentRegistry.findByKey(aiStep.getAgentKey())
                .map(def -> def.getOutputs())
                .orElseThrow(() -> buildException(stepId, fromStepId, key, "not-produced"));
        if (!outputs.contains(key)) {
            throw buildException(stepId, fromStepId, key, "not-produced");
        }
    }

    private Set<String> computeAncestors(String stepId, Map<String, Set<String>> reverseEdges) {
        Set<String> ancestors = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(stepId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String parent : reverseEdges.getOrDefault(current, Set.of())) {
                if (ancestors.add(parent)) {
                    queue.add(parent);
                }
            }
        }
        return ancestors;
    }

    private static Map<String, SquadStepRequest> buildStepMap(List<SquadStepRequest> steps) {
        Map<String, SquadStepRequest> map = new HashMap<>();
        for (SquadStepRequest step : steps) {
            map.put(step.getId(), step);
        }
        return map;
    }

    private static Map<String, Set<String>> buildReverseEdges(List<SquadEdgeRequest> edges) {
        Map<String, Set<String>> reverse = new HashMap<>();
        for (SquadEdgeRequest edge : edges) {
            reverse.computeIfAbsent(edge.getTargetStepId(), k -> new HashSet<>())
                    .add(edge.getSourceStepId());
        }
        return reverse;
    }

    private static void reject(String stepId, String fromStepId, String key, String reason) {
        throw buildException(stepId, fromStepId, key, reason);
    }

    private static ResponseStatusException buildException(String stepId, String fromStepId, String key,
                                                          String reason) {
        String message = String.format("stepId=%s fromStepId=%s key=%s reason=%s", stepId, fromStepId, key, reason);
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
