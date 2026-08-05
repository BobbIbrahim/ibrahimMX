package com.murex.mxorbit.squadorchestrator.config.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Temporal Spring Boot starter auto-configures a {@link WorkflowClient} but not a
 * {@link ScheduleClient}, so Autopilot scheduling would otherwise have nothing to inject.
 */
@Configuration
public class ScheduleClientConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ScheduleClient scheduleClient(WorkflowServiceStubs workflowServiceStubs, WorkflowClient workflowClient) {
		ScheduleClientOptions options = ScheduleClientOptions.newBuilder()
				.setNamespace(workflowClient.getOptions().getNamespace())
				.setDataConverter(workflowClient.getOptions().getDataConverter()).build();

		return ScheduleClient.newInstance(workflowServiceStubs, options);
	}
}
