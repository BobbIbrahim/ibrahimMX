package com.murex.mxorbit.squadorchestrator.core.squad.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class StepInputRefDeserializer extends JsonDeserializer<StepInputRef> {

	@Override
	public StepInputRef deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonNode node = p.getCodec().readTree(p);

		StepInputRefSourceType sourceType = StepInputRefSourceType.STEP_OUTPUT;
		if (node.has("sourceType") && node.get("sourceType").isTextual()) {
			try {
				sourceType = StepInputRefSourceType.valueOf(node.get("sourceType").asText());
			} catch (IllegalArgumentException e) {
				sourceType = StepInputRefSourceType.STEP_OUTPUT;
			}
		}

		String targetInput = node.has("targetInput") ? node.get("targetInput").asText(null) : null;
		String fromStepId = node.has("fromStepId") ? node.get("fromStepId").asText(null) : null;
		String key = node.has("key") ? node.get("key").asText(null) : null;

		return StepInputRef.builder().sourceType(sourceType).targetInput(targetInput).fromStepId(fromStepId).key(key)
				.build();
	}
}
