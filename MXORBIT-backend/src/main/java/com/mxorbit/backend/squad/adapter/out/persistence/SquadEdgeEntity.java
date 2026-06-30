package com.mxorbit.backend.squad.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "squad_edges")
@Getter
@Setter
public class SquadEdgeEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private SquadEntity squad;

    @Column(name = "frontend_edge_id", nullable = false)
    private String frontendEdgeId;

    @Column(name = "source_step_id", nullable = false)
    private String sourceStepId;

    @Column(name = "target_step_id", nullable = false)
    private String targetStepId;
}