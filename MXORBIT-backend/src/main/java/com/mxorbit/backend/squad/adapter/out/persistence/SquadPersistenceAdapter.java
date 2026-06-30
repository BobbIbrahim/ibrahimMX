package com.mxorbit.backend.squad.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxorbit.backend.squad.application.port.out.LoadSquadsPort;
import com.mxorbit.backend.squad.application.port.out.SaveSquadPort;
import com.mxorbit.backend.squad.domain.model.Squad;
import com.mxorbit.backend.squad.domain.model.SquadEdge;
import com.mxorbit.backend.squad.domain.model.SquadPosition;
import com.mxorbit.backend.squad.domain.model.SquadStep;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SquadPersistenceAdapter implements SaveSquadPort, LoadSquadsPort {

    private final SquadRepository squadRepository;
    private final ObjectMapper objectMapper;

    public SquadPersistenceAdapter(
            SquadRepository squadRepository,
            ObjectMapper objectMapper
    ) {
        this.squadRepository = squadRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Squad saveSquad(Squad squad) {
        SquadEntity entity = toEntity(squad);
        SquadEntity savedEntity = squadRepository.save(entity);

        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Squad> loadSquads() {
        return squadRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private SquadEntity toEntity(Squad squad) {
        SquadEntity entity = new SquadEntity();

        entity.setFrontendDraftId(squad.frontendDraftId());
        entity.setName(squad.name());
        entity.setDescription(squad.description());
        entity.setType(squad.type());
        entity.setProjectKey(squad.projectKey());
        entity.setStatus(squad.status());

        squad.steps().forEach(step -> entity.addStep(toStepEntity(step)));
        squad.edges().forEach(edge -> entity.addEdge(toEdgeEntity(edge)));

        return entity;
    }

    private SquadStepEntity toStepEntity(SquadStep step) {
        SquadStepEntity entity = new SquadStepEntity();

        entity.setFrontendStepId(step.frontendStepId());
        entity.setName(step.name());
        entity.setDescription(step.description());
        entity.setAssignedAgentId(step.assignedAgentId());
        entity.setParametersJson(toJson(step.parameters()));
        entity.setPositionX(step.position().x());
        entity.setPositionY(step.position().y());

        return entity;
    }

    private SquadEdgeEntity toEdgeEntity(SquadEdge edge) {
        SquadEdgeEntity entity = new SquadEdgeEntity();

        entity.setFrontendEdgeId(edge.frontendEdgeId());
        entity.setSourceStepId(edge.sourceStepId());
        entity.setTargetStepId(edge.targetStepId());

        return entity;
    }

    private Squad toDomain(SquadEntity entity) {
        return new Squad(
                entity.getId(),
                entity.getFrontendDraftId(),
                entity.getName(),
                entity.getDescription(),
                entity.getType(),
                entity.getProjectKey(),
                entity.getStatus(),
                toDomainSteps(entity.getSteps()),
                toDomainEdges(entity.getEdges()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private List<SquadStep> toDomainSteps(List<SquadStepEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(entity -> new SquadStep(
                        entity.getId(),
                        entity.getFrontendStepId(),
                        entity.getName(),
                        entity.getDescription(),
                        entity.getAssignedAgentId(),
                        fromJson(entity.getParametersJson()),
                        new SquadPosition(
                                entity.getPositionX(),
                                entity.getPositionY()
                        )
                ))
                .toList();
    }

    private List<SquadEdge> toDomainEdges(List<SquadEdgeEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(entity -> new SquadEdge(
                        entity.getId(),
                        entity.getFrontendEdgeId(),
                        entity.getSourceStepId(),
                        entity.getTargetStepId()
                ))
                .toList();
    }

    private String toJson(Map<String, Object> parameters) {
        Map<String, Object> safeParameters =
                parameters == null ? Collections.emptyMap() : parameters;

        try {
            return objectMapper.writeValueAsString(safeParameters);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to serialize squad step parameters",
                    exception
            );
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize squad step parameters",
                    exception
            );
        }
    }
}