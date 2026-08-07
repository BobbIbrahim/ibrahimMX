package com.murex.mxorbit.squadorchestrator.infra.persistence.automation;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.infra.persistence.automation.entity.AutomationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRepository extends JpaRepository<AutomationEntity, UUID> {

	List<AutomationEntity> findByAssigneeTypeAndAssigneeId(AssigneeType assigneeType, String assigneeId);
}
