package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import com.murex.mxorbit.squadorchestrator.config.AgentServiceProperties;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class HttpAgentExecutor implements AgentExecutor {

	private final RestClient restClient;

	@Autowired
	public HttpAgentExecutor(AgentServiceProperties agentServiceProperties) {
		String agentServiceBaseUrl = agentServiceProperties.getBaseUrl();
		if (!StringUtils.hasText(agentServiceBaseUrl)) {
			throw new IllegalArgumentException("agent-service.base-url must not be blank.");
		}

		this.restClient = RestClient.builder().baseUrl(stripTrailingSlash(agentServiceBaseUrl)).build();
	}

	HttpAgentExecutor(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public Map<String, Object> execute(String agentKey, String stepName, Map<String, Object> input) {
		AgentExecuteResponse response;
		try {
			response = restClient.post().uri("/agents/{agentId}/execute", agentKey)
					.contentType(MediaType.APPLICATION_JSON).body(new AgentExecuteRequest(input)).retrieve()
					.body(AgentExecuteResponse.class);
		} catch (ResourceAccessException exception) {
			throw new AgentExecutionException(
					String.format("Agent execution network failure. agentId=%s stepName=%s reason=%s", agentKey,
							stepName, exception.getMessage()),
					exception);
		} catch (RestClientResponseException exception) {
			throw new AgentExecutionException(
					String.format("Agent execution HTTP failure. agentId=%s stepName=%s status=%d body=%s", agentKey,
							stepName, exception.getStatusCode().value(), exception.getResponseBodyAsString()),
					exception);
		} catch (RestClientException exception) {
			throw new AgentExecutionException(
					String.format("Agent execution response parsing failure. agentId=%s stepName=%s reason=%s",
							agentKey, stepName, exception.getMessage()),
					exception);
		}

		if (response == null || response.output() == null) {
			throw new AgentExecutionException(String.format(
					"Agent execution invalid response payload. agentId=%s stepName=%s reason=missing-output", agentKey,
					stepName));
		}

		log.debug("Agent execution completed via HTTP. agentId: {}, stepName: {}, output: {}", agentKey, stepName,
				response.output());
		return response.output();
	}

	private static String stripTrailingSlash(String baseUrl) {
		if (baseUrl.endsWith("/")) {
			return baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl;
	}

	private record AgentExecuteRequest(Map<String, Object> input) {
	}

	private record AgentExecuteResponse(Map<String, Object> output) {
	}
}
