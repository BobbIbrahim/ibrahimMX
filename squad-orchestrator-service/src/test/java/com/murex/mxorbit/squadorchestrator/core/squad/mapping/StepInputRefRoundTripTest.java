package com.murex.mxorbit.squadorchestrator.core.squad.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murex.mxorbit.squadorchestrator.api.squad.mapper.SquadApiMapper;
import com.murex.mxorbit.squadorchestrator.api.squad.request.AiAgentStepApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.request.CreateSquadApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.response.AiAgentStepApiResponse;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.mapper.SquadCreatorMapper;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadStepEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.mapper.SquadPersistenceMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class StepInputRefRoundTripTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	private final SquadApiMapper apiMapper = Mappers.getMapper(SquadApiMapper.class);

	private final SquadCreatorMapper creatorMapper = Mappers.getMapper(SquadCreatorMapper.class);

	private final SquadPersistenceMapper persistenceMapper = Mappers.getMapper(SquadPersistenceMapper.class);

	@Test
	void shouldPreserveTargetInputThroughApiAndDomainMappings() {
		StepInputRef ref = ref("step-1", "message", "requirements");
		AiAgentStepApiRequest apiRequest = apiStep("step-2", "Step 2", "test-weaver", ref);

		CreateSquadApiRequest createSquadApiRequest = CreateSquadApiRequest.builder().name("Squad")
				.description("Round trip").type("hardcoded-flow").steps(List.of(apiRequest)).edges(List.of()).build();

		CreateSquadRequest createRequest = apiMapper.toSquadCreateRequest(createSquadApiRequest);
		AiAgentStepRequest stepRequest = (AiAgentStepRequest) createRequest.getSteps().get(0);
		assertEquals("requirements", stepRequest.getInputRefs().get(0).getTargetInput());

		CreateSquadStoreRequest storeRequest = creatorMapper.toCreateSquadStoreRequest(createRequest);
		assertEquals("requirements",
				((AiAgentStep) storeRequest.getSteps().get(0)).getInputRefs().get(0).getTargetInput());

		AiAgentStep domainStep = creatorMapper.toAiAgentStep(stepRequest);
		assertEquals("requirements", domainStep.getInputRefs().get(0).getTargetInput());

		AiAgentStepApiResponse response = (AiAgentStepApiResponse) apiMapper.toAiAgentStepApiResponse(domainStep);
		assertEquals("requirements", response.getInputRefs().get(0).getTargetInput());
	}

	@Test
	void shouldPreserveTargetInputThroughPersistenceRoundTrip() throws Exception {
		AiAgentStep domainStep = AiAgentStep.builder().id("step-2").name("Step 2").agentKey("test-weaver")
				.inputRefs(List.of(ref("step-1", "message", "testContext"))).build();

		SquadStepEntity entity = persistenceMapper.buildStepEntity(domainStep, "squad-1");
		String json = objectMapper.writeValueAsString(entity.getInputRefs());
		List<StepInputRef> persistedInputRefs = objectMapper.readValue(json, new TypeReference<List<StepInputRef>>() {
		});

		SquadStepEntity reloadedEntity = SquadStepEntity.builder().id(entity.getId()).name(entity.getName())
				.type(entity.getType()).config(Map.of("agentKey", entity.getConfig().get("agentKey")))
				.inputRefs(persistedInputRefs).build();

		SquadStep reloadedStep = persistenceMapper.toStep(reloadedEntity);
		assertEquals("testContext", reloadedStep.getInputRefs().get(0).getTargetInput());
	}

	@Test
	void shouldDeserializeLegacyInputRefWithoutTargetInput() throws Exception {
		StepInputRef ref = objectMapper.readValue("{\"fromStepId\":\"step-1\",\"key\":\"message\"}",
				StepInputRef.class);

		assertNull(ref.getTargetInput());
	}

	@Test
	void shouldLeaveStepInputRefsListSerializable() throws Exception {
		List<StepInputRef> refs = List.of(ref("step-1", "message", "context"));

		String json = objectMapper.writeValueAsString(refs);
		assertTrue(json.contains("targetInput"));
		assertEquals("context", objectMapper.readValue(json, new TypeReference<List<StepInputRef>>() {
		}).get(0).getTargetInput());
	}

	private static AiAgentStepApiRequest apiStep(String id, String name, String agentKey, StepInputRef... inputRefs) {
		AiAgentStepApiRequest step = new AiAgentStepApiRequest();
		step.setId(id);
		step.setName(name);
		step.setAgentKey(agentKey);
		step.setInputRefs(List.of(inputRefs));
		return step;
	}

	private static StepInputRef ref(String fromStepId, String key, String targetInput) {
		return StepInputRef.builder().fromStepId(fromStepId).key(key).targetInput(targetInput).build();
	}
}
