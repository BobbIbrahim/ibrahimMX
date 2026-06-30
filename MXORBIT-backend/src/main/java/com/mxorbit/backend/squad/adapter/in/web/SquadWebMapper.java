package com.mxorbit.backend.squad.adapter.in.web;

import com.mxorbit.backend.squad.adapter.in.web.dto.SquadCreateRequest;
import com.mxorbit.backend.squad.adapter.in.web.dto.SquadEdgeResponse;
import com.mxorbit.backend.squad.adapter.in.web.dto.SquadPositionRequest;
import com.mxorbit.backend.squad.adapter.in.web.dto.SquadResponse;
import com.mxorbit.backend.squad.adapter.in.web.dto.SquadStepResponse;
import com.mxorbit.backend.squad.domain.model.Squad;
import com.mxorbit.backend.squad.domain.model.SquadEdge;
import com.mxorbit.backend.squad.domain.model.SquadPosition;
import com.mxorbit.backend.squad.domain.model.SquadStep;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SquadWebMapper {

    public Squad toDomain(SquadCreateRequest request) {
        return new Squad(
                null,
                request.id(),
                request.name(),
                request.description(),
                request.type(),
                request.projectKey(),
                null,
                toDomainSteps(request),
                toDomainEdges(request),
                null,
                null
        );
    }

    public SquadResponse toResponse(Squad squad) {
        return new SquadResponse(
                squad.id(),
                squad.frontendDraftId(),
                squad.name(),
                squad.description(),
                squad.type(),
                squad.projectKey(),
                squad.status(),
                toStepResponses(squad.steps()),
                toEdgeResponses(squad.edges()),
                squad.createdAt(),
                squad.updatedAt()
        );
    }

    private List<SquadStep> toDomainSteps(SquadCreateRequest request) {
        return request.steps().stream()
                .map(step -> new SquadStep(
                        null,
                        step.id(),
                        step.name(),
                        step.description(),
                        step.assignedAgentId(),
                        step.parameters(),
                        new SquadPosition(
                                step.position().x(),
                                step.position().y()
                        )
                ))
                .toList();
    }

    private List<SquadEdge> toDomainEdges(SquadCreateRequest request) {
        return request.edges().stream()
                .map(edge -> new SquadEdge(
                        null,
                        edge.id(),
                        edge.sourceStepId(),
                        edge.targetStepId()
                ))
                .toList();
    }

    private List<SquadStepResponse> toStepResponses(List<SquadStep> steps) {
        return steps.stream()
                .map(step -> new SquadStepResponse(
                        step.id(),
                        step.frontendStepId(),
                        step.name(),
                        step.description(),
                        step.assignedAgentId(),
                        step.parameters(),
                        new SquadPositionRequest(
                                step.position().x(),
                                step.position().y()
                        )
                ))
                .toList();
    }

    private List<SquadEdgeResponse> toEdgeResponses(List<SquadEdge> edges) {
        return edges.stream()
                .map(edge -> new SquadEdgeResponse(
                        edge.id(),
                        edge.frontendEdgeId(),
                        edge.sourceStepId(),
                        edge.targetStepId()
                ))
                .toList();
    }
}