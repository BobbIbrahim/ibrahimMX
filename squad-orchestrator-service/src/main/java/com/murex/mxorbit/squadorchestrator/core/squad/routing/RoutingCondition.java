package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import java.util.List;

/** Parsed, immutable form of an edge routing condition. */
record RoutingCondition(LogicalOperator logicalOperator, List<Rule> rules) {

	enum LogicalOperator {
		AND, OR
	}

	enum Operator {

		EQUALS("equals"), NOT_EQUALS("notEquals"), IN("in"), CONTAINS("contains");

		private final String expression;

		Operator(String expression) {
			this.expression = expression;
		}

		String expression() {
			return expression;
		}

		static Operator fromExpression(String expression) {
			for (Operator operator : values()) {
				if (operator.expression.equals(expression)) {
					return operator;
				}
			}

			throw new InvalidRoutingConditionException("Unsupported routing condition operator: '" + expression + "'.");
		}
	}

	/**
	 * {@code pathSegments} is pre-split at parse time so evaluation never re-parses
	 * the field path.
	 */
	record Rule(List<String> pathSegments, Operator operator, Object expectedValue, List<Object> expectedValues) {
	}
}
