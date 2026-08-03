package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class SquadRoutingConditionEvaluator {

	private static final String OUTPUT_PREFIX = "output.";

	private static final Pattern LOGICAL_AND_PATTERN = Pattern.compile("\\s+and\\s+");
	private static final Pattern LOGICAL_OR_PATTERN = Pattern.compile("\\s+or\\s+");

	private static final Pattern SIMPLE_CONDITION_PATTERN = Pattern
			.compile("^(output\\.[A-Za-z][A-Za-z0-9_.]*)\\s+(equals|notEquals|in|contains)\\s+(.+)$");

	public void validate(String condition) {
		parse(condition);
	}

	public boolean evaluate(Map<String, Object> sourceStepOutput, String condition) {
		ParsedCondition parsedCondition = parse(condition);

		if (sourceStepOutput == null) {
			return false;
		}

		if (parsedCondition.logicalOperator() == null) {
			return evaluateRule(sourceStepOutput, parsedCondition.rules().get(0));
		}

		if (parsedCondition.logicalOperator() == LogicalOperator.AND) {
			for (ConditionRule rule : parsedCondition.rules()) {
				if (!evaluateRule(sourceStepOutput, rule)) {
					return false;
				}
			}
			return true;
		}

		for (ConditionRule rule : parsedCondition.rules()) {
			if (evaluateRule(sourceStepOutput, rule)) {
				return true;
			}
		}

		return false;
	}

	private ParsedCondition parse(String condition) {
		if (condition == null || condition.isBlank()) {
			throw invalidCondition("Routing condition must not be blank.");
		}

		String normalizedCondition = condition.trim();

		if (normalizedCondition.contains("(") || normalizedCondition.contains(")")) {
			throw invalidCondition("Routing condition must not contain parentheses.");
		}

		boolean containsAnd = LOGICAL_AND_PATTERN.matcher(normalizedCondition).find();
		boolean containsOr = LOGICAL_OR_PATTERN.matcher(normalizedCondition).find();

		if (containsAnd && containsOr) {
			throw invalidCondition("Routing condition must not mix 'and' and 'or'.");
		}

		LogicalOperator logicalOperator = null;
		String[] expressions;

		if (containsAnd) {
			logicalOperator = LogicalOperator.AND;
			expressions = LOGICAL_AND_PATTERN.split(normalizedCondition, -1);
		} else if (containsOr) {
			logicalOperator = LogicalOperator.OR;
			expressions = LOGICAL_OR_PATTERN.split(normalizedCondition, -1);
		} else {
			expressions = new String[]{normalizedCondition};
		}

		List<ConditionRule> rules = new ArrayList<>();
		for (String expression : expressions) {
			if (expression == null || expression.isBlank()) {
				throw invalidCondition("Routing condition contains an empty rule.");
			}

			rules.add(parseRule(expression.trim()));
		}

		if (rules.isEmpty()) {
			throw invalidCondition("Routing condition must contain at least one rule.");
		}

		return new ParsedCondition(logicalOperator, List.copyOf(rules));
	}

	private ConditionRule parseRule(String expression) {
		Matcher matcher = SIMPLE_CONDITION_PATTERN.matcher(expression);
		if (!matcher.matches()) {
			throw invalidCondition("Invalid routing condition rule: '" + expression + "'.");
		}

		String fieldPath = matcher.group(1);
		ConditionOperator operator = ConditionOperator.fromExpression(matcher.group(2));
		String rawExpectedValue = matcher.group(3).trim();

		if (rawExpectedValue.isEmpty()) {
			throw invalidCondition("Routing condition rule '" + expression + "' must have an expected value.");
		}

		String outputPath = fieldPath.substring(OUTPUT_PREFIX.length());
		validateOutputPath(outputPath, expression);

		if (operator == ConditionOperator.IN) {
			List<Object> expectedValues = parseList(rawExpectedValue, expression);
			return new ConditionRule(outputPath, operator, null, expectedValues);
		}

		if (rawExpectedValue.startsWith("[") || rawExpectedValue.endsWith("]")) {
			throw invalidCondition(
					"Routing condition rule '" + expression + "' uses a list with an unsupported operator.");
		}

		Object expectedValue = parseScalar(rawExpectedValue, expression);
		return new ConditionRule(outputPath, operator, expectedValue, List.of());
	}

	private void validateOutputPath(String outputPath, String expression) {
		if (outputPath.isBlank()) {
			throw invalidCondition("Routing condition rule '" + expression + "' must reference an output field.");
		}

		String[] pathSegments = outputPath.split("\\.", -1);
		for (String pathSegment : pathSegments) {
			if (pathSegment.isBlank()) {
				throw invalidCondition(
						"Routing condition rule '" + expression + "' contains an invalid output field path.");
			}
		}
	}

	private List<Object> parseList(String rawExpectedValue, String expression) {
		if (!rawExpectedValue.startsWith("[") || !rawExpectedValue.endsWith("]")) {
			throw invalidCondition(
					"Routing condition rule '" + expression + "' must use a bracketed list for operator 'in'.");
		}

		String listContent = rawExpectedValue.substring(1, rawExpectedValue.length() - 1).trim();
		if (listContent.isEmpty()) {
			throw invalidCondition("Routing condition rule '" + expression + "' must not use an empty list.");
		}

		String[] rawItems = listContent.split(",", -1);
		List<Object> values = new ArrayList<>();

		for (String rawItem : rawItems) {
			String item = rawItem.trim();
			if (item.isEmpty()) {
				throw invalidCondition("Routing condition rule '" + expression + "' contains an empty list value.");
			}

			values.add(parseScalar(item, expression));
		}

		return List.copyOf(values);
	}

	private Object parseScalar(String rawValue, String expression) {
		String value = rawValue.trim();

		if (value.isEmpty()) {
			throw invalidCondition("Routing condition rule '" + expression + "' must have an expected value.");
		}

		if (isQuoted(value)) {
			return value.substring(1, value.length() - 1);
		}

		if (value.startsWith("\"") || value.endsWith("\"") || value.startsWith("'") || value.endsWith("'")) {
			throw invalidCondition("Routing condition rule '" + expression + "' contains an invalid quoted value.");
		}

		if ("true".equals(value)) {
			return Boolean.TRUE;
		}

		if ("false".equals(value)) {
			return Boolean.FALSE;
		}

		if ("null".equals(value)) {
			return null;
		}

		if (isNumber(value)) {
			try {
				return new BigDecimal(value);
			} catch (NumberFormatException exception) {
				throw invalidCondition(
						"Routing condition rule '" + expression + "' contains an invalid numeric value.");
			}
		}

		if (containsWhitespace(value)) {
			throw invalidCondition(
					"Routing condition rule '" + expression + "' must quote string values containing whitespace.");
		}

		return value;
	}

	private boolean evaluateRule(Map<String, Object> sourceStepOutput, ConditionRule rule) {
		ResolvedValue resolvedValue = resolveValue(sourceStepOutput, rule.outputPath());

		if (!resolvedValue.found()) {
			return false;
		}

		Object actualValue = resolvedValue.value();

		return switch (rule.operator()) {
			case EQUALS -> valuesEqual(actualValue, rule.expectedValue());
			case NOT_EQUALS -> !valuesEqual(actualValue, rule.expectedValue());
			case IN -> isIn(actualValue, rule.expectedValues());
			case CONTAINS -> contains(actualValue, rule.expectedValue());
		};
	}

	private ResolvedValue resolveValue(Map<String, Object> sourceStepOutput, String outputPath) {
		if (sourceStepOutput == null) {
			return ResolvedValue.notFound();
		}

		String[] pathSegments = outputPath.split("\\.");
		Object currentValue = sourceStepOutput;

		for (String pathSegment : pathSegments) {
			if (!(currentValue instanceof Map<?, ?> currentMap)) {
				return ResolvedValue.notFound();
			}

			if (!currentMap.containsKey(pathSegment)) {
				return ResolvedValue.notFound();
			}

			currentValue = currentMap.get(pathSegment);
		}

		return ResolvedValue.found(currentValue);
	}

	private boolean isIn(Object actualValue, List<Object> expectedValues) {
		for (Object expectedValue : expectedValues) {
			if (valuesEqual(actualValue, expectedValue)) {
				return true;
			}
		}

		return false;
	}

	private boolean contains(Object actualValue, Object expectedValue) {
		if (actualValue instanceof String actualString && expectedValue instanceof String expectedString) {
			return actualString.contains(expectedString);
		}

		if (actualValue instanceof Collection<?> actualCollection) {
			for (Object item : actualCollection) {
				if (valuesEqual(item, expectedValue)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean valuesEqual(Object actualValue, Object expectedValue) {
		if (actualValue == null || expectedValue == null) {
			return Objects.equals(actualValue, expectedValue);
		}

		if (actualValue instanceof Number actualNumber && expectedValue instanceof BigDecimal expectedNumber) {
			return toBigDecimal(actualNumber).compareTo(expectedNumber) == 0;
		}

		if (actualValue instanceof BigDecimal actualNumber && expectedValue instanceof Number expectedNumber) {
			return actualNumber.compareTo(toBigDecimal(expectedNumber)) == 0;
		}

		if (actualValue instanceof Number actualNumber && expectedValue instanceof Number expectedNumber) {
			return toBigDecimal(actualNumber).compareTo(toBigDecimal(expectedNumber)) == 0;
		}

		if (!actualValue.getClass().equals(expectedValue.getClass())) {
			return false;
		}

		return actualValue.equals(expectedValue);
	}

	private BigDecimal toBigDecimal(Number value) {
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal;
		}

		return new BigDecimal(value.toString());
	}

	private boolean isQuoted(String value) {
		return value.length() >= 2
				&& ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")));
	}

	private boolean isNumber(String value) {
		return value.matches("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?");
	}

	private boolean containsWhitespace(String value) {
		for (int index = 0; index < value.length(); index++) {
			if (Character.isWhitespace(value.charAt(index))) {
				return true;
			}
		}

		return false;
	}

	private IllegalArgumentException invalidCondition(String message) {
		return new IllegalArgumentException(message);
	}

	private enum LogicalOperator {
		AND, OR
	}

	private enum ConditionOperator {
		EQUALS("equals"), NOT_EQUALS("notEquals"), IN("in"), CONTAINS("contains");

		private final String expression;

		ConditionOperator(String expression) {
			this.expression = expression;
		}

		private static ConditionOperator fromExpression(String expression) {
			for (ConditionOperator operator : values()) {
				if (operator.expression.equals(expression)) {
					return operator;
				}
			}

			throw new IllegalArgumentException("Unsupported routing condition operator: '" + expression + "'.");
		}
	}

	private record ParsedCondition(LogicalOperator logicalOperator, List<ConditionRule> rules) {
	}

	private record ConditionRule(String outputPath, ConditionOperator operator, Object expectedValue,
			List<Object> expectedValues) {
	}

	private record ResolvedValue(boolean found, Object value) {

		private static ResolvedValue found(Object value) {
			return new ResolvedValue(true, value);
		}

		private static ResolvedValue notFound() {
			return new ResolvedValue(false, null);
		}
	}
}
