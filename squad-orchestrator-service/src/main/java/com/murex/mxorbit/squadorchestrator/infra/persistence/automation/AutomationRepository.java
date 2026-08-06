package com.murex.mxorbit.squadorchestrator.infra.persistence.automation;

import com.murex.mxorbit.squadorchestrator.infra.persistence.automation.entity.AutomationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRepository extends JpaRepository<AutomationEntity, UUID> {
}
