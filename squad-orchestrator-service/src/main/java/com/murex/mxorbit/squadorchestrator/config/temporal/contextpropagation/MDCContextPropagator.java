package com.murex.mxorbit.squadorchestrator.config.temporal.contextpropagation;

import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import io.temporal.common.converter.GlobalDataConverter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MDCContextPropagator implements ContextPropagator {

	private static final List<String> temporalReservedKeys = List.of("WorkflowType", "TaskQueue", "WorkflowId", "RunId",
			"Namespace");
	private static final String CONTEXT_KEY = "mdc";

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public Map<String, Payload> serializeContext(Object context) {
		Map<String, Payload> serializedContext = new HashMap<>();
		toPayload(context).ifPresent(p -> serializedContext.put(CONTEXT_KEY, p));
		return serializedContext;
	}

	@Override
	public Object deserializeContext(Map<String, Payload> header) {
		if (!header.containsKey(CONTEXT_KEY)) {
			return Collections.emptyMap();
		}

		Payload payload = header.get(CONTEXT_KEY);
		return fromPayload(payload);
	}

	@Override
	public Object getCurrentContext() {
		return Optional.ofNullable(MDC.getCopyOfContextMap()).map(MDCContextPropagator::getMap).orElse(new HashMap<>());
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setCurrentContext(Object context) {
		Map<String, String> contextMap = (Map<String, String>) context;
		for (Map.Entry<String, String> entry : contextMap.entrySet()) {
			MDC.put(entry.getKey(), entry.getValue());
		}
	}

	private static Map<String, String> getMap(Map<String, String> m) {
		return m.entrySet().stream().filter(e -> !temporalReservedKeys.contains(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> fromPayload(Payload payload) {
		return GlobalDataConverter.get().fromPayload(payload, Map.class, Map.class);
	}

	private static Optional<Payload> toPayload(Object value) {
		return GlobalDataConverter.get().toPayload(value);
	}
}
