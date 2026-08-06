package com.murex.mxorbit.squadorchestrator.api.automation;

import com.murex.mxorbit.squadorchestrator.api.automation.request.AutomationApiRequest;
import com.murex.mxorbit.squadorchestrator.api.automation.response.AutomationApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("automations")
public interface AutomationApi {

	@GetMapping
	@Operation(summary = "Get all automations")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Automations retrieved"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<List<AutomationApiResponse>> getAutomations();

	@GetMapping("/{automationId}")
	@Operation(summary = "Get an automation by ID")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Automation retrieved"),
			@ApiResponse(responseCode = "400", description = "Unsupported assignee type"),
			@ApiResponse(responseCode = "404", description = "Automation or assignee not found"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<AutomationApiResponse> getAutomationById(@PathVariable UUID automationId);

	@PostMapping
	@Operation(summary = "Create an automation")
	@ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Automation created"),
			@ApiResponse(responseCode = "400", description = "Invalid request, unsupported assignee type (AGENT) or invalid Squad root input"),
			@ApiResponse(responseCode = "404", description = "Assignee not found"),
			@ApiResponse(responseCode = "409", description = "Temporal Schedule ID conflict"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<AutomationApiResponse> createAutomation(@RequestBody @Valid AutomationApiRequest request);

	@PutMapping("/{automationId}")
	@Operation(summary = "Update an automation")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Automation updated"),
			@ApiResponse(responseCode = "400", description = "Invalid request, unsupported assignee type or invalid input"),
			@ApiResponse(responseCode = "404", description = "Automation or assignee not found"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<AutomationApiResponse> updateAutomation(@PathVariable UUID automationId,
			@RequestBody @Valid AutomationApiRequest request);

	@PostMapping("/{automationId}/pause")
	@Operation(summary = "Pause an automation")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Automation paused"),
			@ApiResponse(responseCode = "404", description = "Automation not found"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<AutomationApiResponse> pauseAutomation(@PathVariable UUID automationId);

	@PostMapping("/{automationId}/resume")
	@Operation(summary = "Resume an automation")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Automation resumed"),
			@ApiResponse(responseCode = "404", description = "Automation not found"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<AutomationApiResponse> resumeAutomation(@PathVariable UUID automationId);

	@DeleteMapping("/{automationId}")
	@Operation(summary = "Delete an automation")
	@ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Automation deleted"),
			@ApiResponse(responseCode = "404", description = "Automation not found"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<Void> deleteAutomation(@PathVariable UUID automationId);

}
