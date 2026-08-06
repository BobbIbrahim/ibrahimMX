package com.murex.mxorbit.squadorchestrator.api.automation;

import com.murex.mxorbit.squadorchestrator.api.automation.mapper.AutomationApiMapper;
import com.murex.mxorbit.squadorchestrator.api.automation.request.AutomationApiRequest;
import com.murex.mxorbit.squadorchestrator.api.automation.response.AutomationApiResponse;
import com.murex.mxorbit.squadorchestrator.core.automation.facade.AutomationFacade;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AutomationController implements AutomationApi {

	private final AutomationFacade automationFacade;
	private final AutomationApiMapper automationApiMapper;

	@Override
	public ResponseEntity<List<AutomationApiResponse>> getAutomations() {
		log.debug("Received request to get all automations");
		return ResponseEntity.ok(automationApiMapper.toAutomationApiResponses(automationFacade.getAutomations()));
	}

	@Override
	public ResponseEntity<AutomationApiResponse> getAutomationById(UUID automationId) {
		log.debug("Received request to get automation by id: {}", automationId);
		return ResponseEntity
				.ok(automationApiMapper.toAutomationApiResponse(automationFacade.getAutomationById(automationId)));
	}

	@Override
	public ResponseEntity<AutomationApiResponse> createAutomation(AutomationApiRequest request) {
		log.debug("Received request to create automation");
		AutomationApiResponse response = automationApiMapper.toAutomationApiResponse(
				automationFacade.createAutomation(automationApiMapper.toCreateAutomationRequest(request)));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Override
	public ResponseEntity<AutomationApiResponse> updateAutomation(UUID automationId, AutomationApiRequest request) {
		log.debug("Received request to update automation with id: {}", automationId);
		AutomationApiResponse response = automationApiMapper.toAutomationApiResponse(automationFacade
				.updateAutomation(automationId, automationApiMapper.toCreateAutomationRequest(request)));
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<AutomationApiResponse> pauseAutomation(UUID automationId) {
		log.debug("Received request to pause automation with id: {}", automationId);
		return ResponseEntity
				.ok(automationApiMapper.toAutomationApiResponse(automationFacade.pauseAutomation(automationId)));
	}

	@Override
	public ResponseEntity<AutomationApiResponse> resumeAutomation(UUID automationId) {
		log.debug("Received request to resume automation with id: {}", automationId);
		return ResponseEntity
				.ok(automationApiMapper.toAutomationApiResponse(automationFacade.resumeAutomation(automationId)));
	}

	@Override
	public ResponseEntity<Void> deleteAutomation(UUID automationId) {
		log.debug("Received request to delete automation with id: {}", automationId);
		return automationFacade.deleteAutomation(automationId) ? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}
}
