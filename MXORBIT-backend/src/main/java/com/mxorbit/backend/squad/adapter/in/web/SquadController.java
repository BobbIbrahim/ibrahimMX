package com.mxorbit.backend.squad.adapter.in.web;

import com.mxorbit.backend.squad.adapter.in.web.dto.SquadCreateRequest;
import com.mxorbit.backend.squad.adapter.in.web.dto.SquadResponse;
import com.mxorbit.backend.squad.application.port.in.CreateSquadUseCase;
import com.mxorbit.backend.squad.application.port.in.GetSquadsUseCase;
import com.mxorbit.backend.squad.domain.model.Squad;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/squads")
@CrossOrigin(origins = "http://localhost:4200")
public class SquadController {

    private final CreateSquadUseCase createSquadUseCase;
    private final GetSquadsUseCase getSquadsUseCase;
    private final SquadWebMapper squadWebMapper;

    public SquadController(
            CreateSquadUseCase createSquadUseCase,
            GetSquadsUseCase getSquadsUseCase,
            SquadWebMapper squadWebMapper
    ) {
        this.createSquadUseCase = createSquadUseCase;
        this.getSquadsUseCase = getSquadsUseCase;
        this.squadWebMapper = squadWebMapper;
    }

    @PostMapping
    public ResponseEntity<SquadResponse> createSquad(
            @Valid @RequestBody SquadCreateRequest request
    ) {
        Squad squadToCreate = squadWebMapper.toDomain(request);
        Squad createdSquad = createSquadUseCase.createSquad(squadToCreate);
        SquadResponse response = squadWebMapper.toResponse(createdSquad);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<SquadResponse>> getSquads() {
        List<SquadResponse> response = getSquadsUseCase.getSquads()
                .stream()
                .map(squadWebMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}