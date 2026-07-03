package com.murex.mxorbit.squadorchestrator.api.squad;

import com.murex.mxorbit.squadorchestrator.api.squad.mapper.SquadApiMapper;
import com.murex.mxorbit.squadorchestrator.api.squad.request.CreateSquadApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadApiResponse;
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
}
