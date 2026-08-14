package com.murex.mxorbit.squadorchestrator.core.squad.run.deleter;

public interface SquadRunDeleter {

	/**
	 * Deletes all runs (Temporal workflow executions and persisted execution
	 * traces) belonging to the given squad. Intended to be called as part of
	 * squad deletion so stale runs no longer show up in the runs dashboard.
	 */
	void deleteSquadRuns(String squadId);

	/**
	 * Deletes a single run (Temporal workflow execution and persisted execution
	 * traces). Returns {@code false} when the run cannot be found.
	 */
	boolean deleteSquadRun(String squadRunId);
}
