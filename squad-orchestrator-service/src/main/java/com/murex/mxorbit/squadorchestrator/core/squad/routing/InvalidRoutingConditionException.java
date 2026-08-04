package com.murex.mxorbit.squadorchestrator.core.squad.routing;

/** Signals a routing condition that cannot be parsed. */
public class InvalidRoutingConditionException extends IllegalArgumentException {

	public InvalidRoutingConditionException(String message) {
		super(message);
	}
}
