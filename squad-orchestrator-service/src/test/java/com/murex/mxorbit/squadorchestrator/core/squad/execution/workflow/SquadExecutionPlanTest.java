package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Proves the existing join-all convergence contract used by
 * {@link SquadExecutionWorkflowImpl}: a normal step's non-default ALWAYS
 * edges are all traversed at once (parallel fan-out, no branch waits on its
 * sibling to be scheduled), and a step with several incoming edges is
 * released -- exactly once -- only after every incoming edge is resolved.
 *
 * <pre>
 *                     +-- ALWAYS --&gt; s2 --+
 *   s1 -- fan-out --&gt; |                    +--&gt; s3
 *                     +-- ALWAYS --&gt; s4 --+
 * </pre>
 */
class SquadExecutionPlanTest {

	private static SquadStep step(String id) {
		return AiAgentStep.builder().id(id).name(id).agentKey("agent").build();
	}

	private static SquadEdge fanOutEdge(String id, String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(id).sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.ALWAYS).priority(1).isDefault(false).build();
	}

	private static SquadGraph diamondGraph() {
		List<SquadStep> steps = List.of(step("s1"), step("s2"), step("s3"), step("s4"));
		List<SquadEdge> edges = List.of(fanOutEdge("e1", "s1", "s2"), fanOutEdge("e2", "s1", "s4"),
				fanOutEdge("e3", "s2", "s3"), fanOutEdge("e4", "s4", "s3"));

		return SquadGraph.build(steps, edges);
	}

	@Test
	void parentCompletionSchedulesBothNormalOutgoingBranchesTogether() {
		SquadExecutionPlan plan = new SquadExecutionPlan(diamondGraph());

		assertEquals(List.of("s1"), plan.releasedStepIds());
		plan.markExecuted("s1");
		plan.traverseAll("s1");

		// Both branches are released in the same batch: scheduling s2 does not
		// require s4 to finish (and vice versa) before it becomes runnable.
		assertEquals(List.of("s2", "s4"), plan.releasedStepIds());
	}

	@Test
	void convergedDestinationDoesNotRunUntilEveryIncomingBranchResolves() {
		SquadExecutionPlan plan = new SquadExecutionPlan(diamondGraph());

		plan.markExecuted("s1");
		plan.traverseAll("s1");
		assertEquals(List.of("s2", "s4"), plan.releasedStepIds());

		// Only s2 has finished and resolved its outgoing edge so far; s4 is still
		// pending (its own release, from the earlier fan-out, is independent of s2).
		plan.markExecuted("s2");
		plan.traverseAll("s2");

		assertEquals(List.of("s4"), plan.releasedStepIds(),
				"s3 must not run before every incoming branch (s4) has resolved");

		// s4 finishes and resolves its outgoing edge too.
		plan.markExecuted("s4");
		plan.traverseAll("s4");

		assertEquals(List.of("s3"), plan.releasedStepIds());
	}

	@Test
	void convergedDestinationExecutesExactlyOnce() {
		SquadExecutionPlan plan = new SquadExecutionPlan(diamondGraph());

		plan.markExecuted("s1");
		plan.traverseAll("s1");
		plan.markExecuted("s2");
		plan.traverseAll("s2");
		plan.markExecuted("s4");
		plan.traverseAll("s4");

		assertEquals(List.of("s3"), plan.releasedStepIds());
		plan.markExecuted("s3");

		// s3 is settled now: it must never be released again, even though its
		// incoming edges remain resolved and traversed.
		assertTrue(plan.releasedStepIds().isEmpty());
		assertTrue(plan.pendingStepIds().isEmpty());
	}
}
