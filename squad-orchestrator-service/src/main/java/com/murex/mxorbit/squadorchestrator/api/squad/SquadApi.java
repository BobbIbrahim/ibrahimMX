package com.murex.mxorbit.squadorchestrator.api.squad;

import com.murex.mxorbit.squadorchestrator.api.squad.request.CreateSquadApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("squads")
public interface SquadApi {

	@PostMapping
	@Operation(summary = "Create a squad")
	@ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Squad created"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<SquadApiResponse> createSquad(@RequestBody @Valid CreateSquadApiRequest request);

	@GetMapping
	@Operation(summary = "Get all squads")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Squads retrieved"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<List<SquadApiResponse>> getSquads();

	@GetMapping("/{squadId}")
	@Operation(summary = "Get a squad by ID")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Squad retrieved"),
			@ApiResponse(responseCode = "404", description = "Squad not found"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<SquadApiResponse> getSquadById(@PathVariable String squadId);

	@PutMapping("/{squadId}")
	@Operation(summary = "Update a squad")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Squad updated"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "404", description = "Squad not found"),
			@ApiResponse(responseCode = "500", description = "Internal error")})
	ResponseEntity<SquadApiResponse> updateSquad(@PathVariable String squadId,
			@RequestBody @Valid CreateSquadApiRequest request);
}
