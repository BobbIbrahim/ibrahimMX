package com.murex.mxorbit.squadorchestrator.core.squad.routing;

public class SquadRoutingDecisionException extends RuntimeException {

	private final SquadRoutingDecision decision;

	public SquadRoutingDecisionException(String message) {
		this(message, null);
	}

	public SquadRoutingDecisionException(String message, SquadRoutingDecision decision) {
		super(message);
		this.decision = decision;
	}

	public SquadRoutingDecision getDecision() {
		return decision;
	}
}
