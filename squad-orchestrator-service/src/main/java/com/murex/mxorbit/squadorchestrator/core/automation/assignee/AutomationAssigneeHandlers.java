package com.murex.mxorbit.squadorchestrator.core.automation.assignee;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Small lookup from {@link AssigneeType} to the
 * {@link AutomationAssigneeHandler} that knows how to validate, name and
 * schedule that kind of assignee. Adding support for a new assignee type is
 * just adding a new handler bean; nothing here needs to change.
 */
@Component
public class AutomationAssigneeHandlers {

	private final Map<AssigneeType, AutomationAssigneeHandler> handlersByType;

	public AutomationAssigneeHandlers(List<AutomationAssigneeHandler> handlers) {
		this.handlersByType = handlers.stream()
				.collect(Collectors.toMap(AutomationAssigneeHandler::supportedType, Function.identity()));
	}

	public AutomationAssigneeHandler getHandler(AssigneeType assigneeType) {
		if (assigneeType == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Automation assigneeType is required.");
		}

		AutomationAssigneeHandler handler = handlersByType.get(assigneeType);
		if (handler == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Automations for assignee type " + assigneeType + " are not supported yet.");
		}

		return handler;
	}
}
