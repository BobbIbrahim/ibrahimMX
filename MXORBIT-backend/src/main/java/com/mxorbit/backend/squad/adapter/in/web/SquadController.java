package com.mxorbit.backend.squad.adapter.in.web;

import com.mxorbit.backend.squad.adapter.in.web.dto.SquadCreateRequest;
import com.mxorbit.backend.squad.adapter.in.web.dto.SquadResponse;
import com.mxorbit.backend.squad.application.port.in.CreateSquadUseCase;
import com.mxorbit.backend.squad.domain.model.Squad;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/squads")
@CrossOrigin(origins = "http://localhost:4200")
public class SquadController {

    private final CreateSquadUseCase createSquadUseCase;
    private final SquadWebMapper squadWebMapper;

    public SquadController(
            CreateSquadUseCase createSquadUseCase,
            SquadWebMapper squadWebMapper
    ) {
        this.createSquadUseCase = createSquadUseCase;
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
}