package com.murex.mxorbit.squadorchestrator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "agent-service")
public class AgentServiceProperties {

	private String baseUrl;
}
