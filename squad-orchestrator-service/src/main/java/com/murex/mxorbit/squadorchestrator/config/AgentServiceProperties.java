package com.murex.mxorbit.squadorchestrator.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "agent-service")
public class AgentServiceProperties {

	private String baseUrl;

	private Duration connectTimeout = Duration.ofSeconds(5);

	/**
	 * Must stay below the agent activity start-to-close timeout so the socket never
	 * outlives the activity.
	 */
	private Duration readTimeout = Duration.ofMinutes(9);
}
