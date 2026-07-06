package com.murex.mxorbit.squadorchestrator.config.temporal.contextpropagation;

import io.temporal.client.WorkflowClientOptions;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@TemporalConfig
@AllArgsConstructor
public class WorkflowClientOptionsCustomizer implements TemporalOptionsCustomizer<WorkflowClientOptions.Builder> {

	private final MDCContextPropagator mdcContextPropagator;

	@Override
	public WorkflowClientOptions.Builder customize(WorkflowClientOptions.Builder optionsBuilder) {
		return optionsBuilder.setContextPropagators(List.of(mdcContextPropagator));
	}
}
