package com.murex.mxorbit.squadorchestrator.infra.persistence.squad;

import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadRepository extends JpaRepository<SquadEntity, String> {
}
