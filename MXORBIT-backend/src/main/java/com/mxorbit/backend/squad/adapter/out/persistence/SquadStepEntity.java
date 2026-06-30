package com.mxorbit.backend.squad.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "squad_steps")
@Getter
@Setter
public class SquadStepEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private SquadEntity squad;

    @Column(name = "frontend_step_id", nullable = false)
    private String frontendStepId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "assigned_agent_id", nullable = false)
    private String assignedAgentId;

    @Lob
    @Column(name = "parameters_json")
    private String parametersJson;

    @Column(name = "position_x", nullable = false)
    private Double positionX;

    @Column(name = "position_y", nullable = false)
    private Double positionY;
}