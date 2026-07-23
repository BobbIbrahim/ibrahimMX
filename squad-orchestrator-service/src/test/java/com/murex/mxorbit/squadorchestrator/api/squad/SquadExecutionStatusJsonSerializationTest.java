package com.murex.mxorbit.squadorchestrator.api.squad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SquadExecutionStatusJsonSerializationTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void shouldSerializeStepTimingFieldsInExecutionStatusResponse() throws Exception {
		SquadStepStatus stepStatus = SquadStepStatus.builder().stepId("step-1").stepName("Step 1")
				.status(SquadStepExecutionStatus.COMPLETED).message("done")
				.startedAt(Instant.parse("2026-07-23T13:11:50Z")).completedAt(Instant.parse("2026-07-23T13:11:53Z"))
				.durationMs(3000L).input(Map.of()).output(Map.of()).build();

		SquadExecutionStatus status = SquadExecutionStatus.builder().squadId("squad-1")
				.overallStatus(WorkflowRunStatus.COMPLETED).steps(List.of(stepStatus)).build();

		String json = objectMapper.writeValueAsString(status);
		JsonNode node = objectMapper.readTree(json).get("steps").get(0);

		assertTrue(node.has("startedAt"));
		assertTrue(node.has("completedAt"));
		assertTrue(node.has("durationMs"));
		assertEquals(3000L, node.get("durationMs").asLong());
	}
}
