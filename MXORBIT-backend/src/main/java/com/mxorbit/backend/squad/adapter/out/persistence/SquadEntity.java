package com.mxorbit.backend.squad.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "squads")
@Getter
@Setter
public class SquadEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "frontend_draft_id")
    private String frontendDraftId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String type;

    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Column(nullable = false)
    private String status;

    @OneToMany(
            mappedBy = "squad",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<SquadStepEntity> steps = new ArrayList<>();

    @OneToMany(
            mappedBy = "squad",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<SquadEdgeEntity> edges = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void addStep(SquadStepEntity step) {
        steps.add(step);
        step.setSquad(this);
    }

    public void addEdge(SquadEdgeEntity edge) {
        edges.add(edge);
        edge.setSquad(this);
    }
}