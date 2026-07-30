package com.murex.mxorbit.squadorchestrator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentServiceProperties.class)
public class AgentServicePropertiesConfiguration {
}
