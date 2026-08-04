package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.RoutingCondition.LogicalOperator;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.RoutingCondition.Rule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SquadRoutingConditionEvaluator {

	private static final int MAX_CACHED_CONDITIONS = 1_000;

	/**
	 * Static so workflow-local instances share one bounded cache instead of one
	 * copy per cached workflow.
	 */
	private static final Map<String, RoutingCondition> PARSED_CONDITION_CACHE = Collections
			.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<String, RoutingCondition> eldest) {
					return size() > MAX_CACHED_CONDITIONS;
				}
			});

	private final RoutingConditionParser parser = new RoutingConditionParser();

	public void validate(String condition) {
		parseCached(condition);
	}

	public boolean evaluate(Map<String, Object> sourceStepOutput, String condition) {
		RoutingCondition routingCondition = parseCached(condition);

		if (sourceStepOutput == null) {
			return false;
		}

		if (routingCondition.logicalOperator() == LogicalOperator.OR) {
			return routingCondition.rules().stream().anyMatch(rule -> evaluateRule(sourceStepOutput, rule));
		}

		return routingCondition.rules().stream().allMatch(rule -> evaluateRule(sourceStepOutput, rule));
	}

	/**
	 * Conditions are static per squad version, so parsing is memoized. Invalid ones
	 * throw and stay uncached.
	 */
	private RoutingCondition parseCached(String condition) {
		if (condition == null || condition.isBlank()) {
			throw new InvalidRoutingConditionException("Routing condition must not be blank.");
		}

		return PARSED_CONDITION_CACHE.computeIfAbsent(condition, parser::parse);
	}

	private boolean evaluateRule(Map<String, Object> sourceStepOutput, Rule rule) {
		ResolvedValue resolvedValue = resolveValue(sourceStepOutput, rule.pathSegments());

		if (!resolvedValue.found()) {
			return false;
		}

		Object actualValue = resolvedValue.value();

		return switch (rule.operator()) {
			case EQUALS -> RoutingConditionValues.equal(actualValue, rule.expectedValue());
			case NOT_EQUALS -> !RoutingConditionValues.equal(actualValue, rule.expectedValue());
			case IN -> RoutingConditionValues.in(actualValue, rule.expectedValues());
			case CONTAINS -> RoutingConditionValues.contains(actualValue, rule.expectedValue());
		};
	}

	/**
	 * Absent is tracked separately from null so {@code equals null} can match a
	 * present null value.
	 */
	private ResolvedValue resolveValue(Map<String, Object> sourceStepOutput, List<String> pathSegments) {
		Object currentValue = sourceStepOutput;

		for (String pathSegment : pathSegments) {
			if (!(currentValue instanceof Map<?, ?> currentMap) || !currentMap.containsKey(pathSegment)) {
				return ResolvedValue.notFound();
			}

			currentValue = currentMap.get(pathSegment);
		}

		return new ResolvedValue(true, currentValue);
	}

	private record ResolvedValue(boolean found, Object value) {

		private static ResolvedValue notFound() {
			return new ResolvedValue(false, null);
		}
	}
}
