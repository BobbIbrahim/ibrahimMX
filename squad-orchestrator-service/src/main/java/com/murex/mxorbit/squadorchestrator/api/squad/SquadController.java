package com.murex.mxorbit.squadorchestrator.api.squad;

import com.murex.mxorbit.squadorchestrator.api.squad.mapper.SquadApiMapper;
import com.murex.mxorbit.squadorchestrator.api.squad.request.CreateSquadApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadApiResponse;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadRunListApiResponse;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadRunApiResponse;
import com.murex.mxorbit.squadorchestrator.core.squad.facade.SquadFacade;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SquadController implements SquadApi {

	private final SquadFacade squadFacade;
	private final SquadApiMapper squadApiMapper;

	@Override
	public ResponseEntity<SquadApiResponse> createSquad(CreateSquadApiRequest request) {
		log.debug("Received request to create squad: {}", request);
		return ResponseEntity.status(HttpStatus.CREATED).body(squadApiMapper
				.toSquadApiResponse(squadFacade.createSquad(squadApiMapper.toSquadCreateRequest(request))));
	}

	@Override
	public ResponseEntity<List<SquadApiResponse>> getSquads() {
		log.debug("Received request to get all squads");
		return ResponseEntity.ok(squadApiMapper.toSquadApiResponses(squadFacade.getSquads()));
	}

	@Override
	public ResponseEntity<SquadApiResponse> getSquadById(String squadId) {
		log.debug("Received request to get squad by id: {}", squadId);
		Optional<SquadApiResponse> squadApiResponse = squadFacade.getSquadById(squadId)
				.map(squadApiMapper::toSquadApiResponse);

		return ResponseEntity.of(squadApiResponse);
	}

	@Override
	public ResponseEntity<SquadApiResponse> updateSquad(String squadId, CreateSquadApiRequest request) {
		log.debug("Received request to update squad with id: {} and request: {}", squadId, request);

		Optional<SquadApiResponse> squadApiResponse = squadFacade
				.updateSquad(squadId, squadApiMapper.toSquadCreateRequest(request))
				.map(squadApiMapper::toSquadApiResponse);

		return ResponseEntity.of(squadApiResponse);
	}

	@Override
	public ResponseEntity<SquadRunApiResponse> startSquadRun(String squadId) {
		log.debug("Received request to start squad run for squad id: {}", squadId);

		return squadFacade.startSquadRun(squadId)
				.map(run -> SquadRunApiResponse.builder().squadId(run.getSquadId()).workflowId(run.getWorkflowId())
						.runId(run.getRunId()).status(run.getStatus()).build())
				.map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<List<SquadRunListApiResponse>> getSquadRuns() {
		log.debug("Received request to get all squad runs");
		return ResponseEntity.ok(squadFacade.getSquadRuns().stream()
				.map(run -> SquadRunListApiResponse.builder().squadId(run.getSquadId()).squadName(run.getSquadName())
						.workflowId(run.getWorkflowId()).runId(run.getRunId()).startedAt(run.getStartedAt())
						.status(run.getStatus()).build())
				.toList());
	}
}
