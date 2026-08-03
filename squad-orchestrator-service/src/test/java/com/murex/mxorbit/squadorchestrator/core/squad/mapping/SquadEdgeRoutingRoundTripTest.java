package com.murex.mxorbit.squadorchestrator.core.squad.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murex.mxorbit.squadorchestrator.api.squad.mapper.SquadApiMapper;
import com.murex.mxorbit.squadorchestrator.api.squad.request.AiAgentStepApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.request.CreateSquadApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.request.SquadEdgeApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadEdgeApiResponse;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.mapper.SquadCreatorMapper;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadEdgeEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.mapper.SquadPersistenceMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SquadEdgeRoutingRoundTripTest {

	private final SquadApiMapper apiMapper = Mappers.getMapper(SquadApiMapper.class);

	private final SquadCreatorMapper creatorMapper = Mappers.getMapper(SquadCreatorMapper.class);

	private final SquadPersistenceMapper persistenceMapper = Mappers.getMapper(SquadPersistenceMapper.class);

	@Test
	void shouldApplyEdgeRoutingDefaultsThroughApiAndDomainMappings() {
		AiAgentStepApiRequest sourceStep = apiStep("step-1", "Step 1", "code-sentinel");
		AiAgentStepApiRequest targetStep = apiStep("step-2", "Step 2", "test-weaver");
		SquadEdgeApiRequest edgeRequest = SquadEdgeApiRequest.builder().sourceStepId("step-1").targetStepId("step-2")
				.build();

		CreateSquadApiRequest createSquadApiRequest = CreateSquadApiRequest.builder().name("Squad")
				.description("Edge defaults").type("hardcoded-flow").steps(List.of(sourceStep, targetStep))
				.edges(List.of(edgeRequest)).build();

		CreateSquadRequest createRequest = apiMapper.toSquadCreateRequest(createSquadApiRequest);
		SquadEdgeRequest mappedEdgeRequest = createRequest.getEdges().get(0);
		assertEquals(SquadEdgeRoutingType.ALWAYS, mappedEdgeRequest.getRoutingType());
		assertNull(mappedEdgeRequest.getCondition());
		assertEquals(100, mappedEdgeRequest.getPriority());
		assertFalse(mappedEdgeRequest.getIsDefault());

		SquadEdge domainEdge = creatorMapper.toEdge(mappedEdgeRequest);
		assertEquals(SquadEdgeRoutingType.ALWAYS, domainEdge.getRoutingType());
		assertNull(domainEdge.getCondition());
		assertEquals(100, domainEdge.getPriority());
		assertFalse(domainEdge.getIsDefault());

		CreateSquadStoreRequest storeRequest = creatorMapper.toCreateSquadStoreRequest(createRequest);
		SquadEdge storeEdge = storeRequest.getEdges().get(0);
		assertEquals(SquadEdgeRoutingType.ALWAYS, storeEdge.getRoutingType());
		assertNull(storeEdge.getCondition());
		assertEquals(100, storeEdge.getPriority());
		assertFalse(storeEdge.getIsDefault());
	}

	@Test
	void shouldPreserveEdgeRoutingMetadataThroughPersistenceRoundTrip() {
		SquadEdge edge = SquadEdge.builder().id("edge-1").sourceStepId("step-1").targetStepId("step-2")
				.routingType(SquadEdgeRoutingType.WHEN).condition("{{step-1.message != null}}").priority(10)
				.isDefault(true).build();

		SquadEdgeEntity entity = persistenceMapper.toEdgeEntity(edge);
		assertEquals(SquadEdgeRoutingType.WHEN, entity.getRoutingType());
		assertEquals("{{step-1.message != null}}", entity.getCondition());
		assertEquals(10, entity.getPriority());
		assertTrue(entity.getIsDefault());

		entity.setRoutingType(null);
		entity.setPriority(null);
		entity.setIsDefault(null);
		SquadEdge reloadedEdge = persistenceMapper.toEdge(entity);
		assertEquals(SquadEdgeRoutingType.ALWAYS, reloadedEdge.getRoutingType());
		assertEquals("{{step-1.message != null}}", reloadedEdge.getCondition());
		assertEquals(100, reloadedEdge.getPriority());
		assertFalse(reloadedEdge.getIsDefault());
	}

	@Test
	void shouldPreserveExplicitRoutingValuesThroughEntityToResponsePath() {
		SquadEdgeEntity entity = SquadEdgeEntity.builder().id("edge-2").sourceStepId("step-a").targetStepId("step-b")
				.routingType(SquadEdgeRoutingType.WHEN).condition("x > 1").priority(7).isDefault(true).build();

		SquadEdge domainEdge = persistenceMapper.toEdge(entity);
		SquadEdgeApiResponse response = apiMapper.toSquadEdgeApiResponse(domainEdge);

		assertEquals(SquadEdgeRoutingType.WHEN, response.getRoutingType());
		assertEquals("x > 1", response.getCondition());
		assertEquals(7, response.getPriority());
		assertTrue(response.getIsDefault());
	}

	@Test
	void shouldResolveDefaultsThroughRequestToEntityPath() {
		AiAgentStepApiRequest sourceStep = apiStep("step-1", "Step 1", "code-sentinel");
		AiAgentStepApiRequest targetStep = apiStep("step-2", "Step 2", "test-weaver");
		SquadEdgeApiRequest edgeRequest = SquadEdgeApiRequest.builder().sourceStepId("step-1").targetStepId("step-2")
				.build();

		CreateSquadApiRequest createSquadApiRequest = CreateSquadApiRequest.builder().name("Squad")
				.description("Edge defaults entity").type("hardcoded-flow").steps(List.of(sourceStep, targetStep))
				.edges(List.of(edgeRequest)).build();

		CreateSquadRequest createRequest = apiMapper.toSquadCreateRequest(createSquadApiRequest);
		SquadEdge domainEdge = creatorMapper.toEdge(createRequest.getEdges().get(0));
		SquadEdgeEntity entity = persistenceMapper.toEdgeEntity(domainEdge);

		assertEquals(SquadEdgeRoutingType.ALWAYS, entity.getRoutingType());
		assertNull(entity.getCondition());
		assertEquals(100, entity.getPriority());
		assertFalse(entity.getIsDefault());
	}

	private static AiAgentStepApiRequest apiStep(String id, String name, String agentKey) {
		AiAgentStepApiRequest step = new AiAgentStepApiRequest();
		step.setId(id);
		step.setName(name);
		step.setAgentKey(agentKey);
		step.setInputRefs(List.of());
		return step;
	}
}
