package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.RoutingCondition.LogicalOperator;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.RoutingCondition.Operator;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.RoutingCondition.Rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recursive-descent-free parser for the restricted routing condition grammar.
 * Deliberately not a script engine: only the documented operator set is
 * accepted, so a condition can never execute arbitrary code.
 */
final class RoutingConditionParser {

	private static final String OUTPUT_PREFIX = "output.";

	private static final Pattern SIMPLE_CONDITION_PATTERN = Pattern
			.compile("^(output\\.[A-Za-z][A-Za-z0-9_.]*)\\s+(equals|notEquals|in|contains)\\s+(.+)$");

	private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?");

	private static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("\\.");

	RoutingCondition parse(String condition) {
		if (condition == null || condition.isBlank()) {
			throw invalidCondition("Routing condition must not be blank.");
		}

		String normalizedCondition = condition.trim();
		List<Segment> segments = splitOnLogicalOperators(normalizedCondition);

		LogicalOperator logicalOperator = resolveLogicalOperator(segments);
		List<Rule> rules = new ArrayList<>();

		for (Segment segment : segments) {
			if (segment.text().isBlank()) {
				throw invalidCondition("Routing condition contains an empty rule.");
			}

			rules.add(parseRule(segment.text().trim()));
		}

		return new RoutingCondition(logicalOperator, List.copyOf(rules));
	}

	/**
	 * Splits on top-level {@code and}/{@code or} only. Quoted regions are skipped
	 * so a string literal may legally contain those keywords.
	 */
	private List<Segment> splitOnLogicalOperators(String condition) {
		List<Segment> segments = new ArrayList<>();
		int segmentStart = 0;
		char openQuote = 0;

		for (int index = 0; index < condition.length(); index++) {
			char character = condition.charAt(index);

			if (openQuote != 0) {
				if (character == openQuote) {
					openQuote = 0;
				}
				continue;
			}

			if (character == '"' || character == '\'') {
				openQuote = character;
				continue;
			}

			if (character == '(' || character == ')') {
				throw invalidCondition("Routing condition must not contain parentheses.");
			}

			LogicalOperator keyword = keywordAt(condition, index);
			if (keyword == null) {
				continue;
			}

			segments.add(new Segment(condition.substring(segmentStart, index), keyword));
			index = skipKeyword(condition, index, keyword) - 1;
			segmentStart = index + 1;
		}

		if (openQuote != 0) {
			throw invalidCondition("Routing condition has an unterminated quoted value.");
		}

		segments.add(new Segment(condition.substring(segmentStart), null));
		return segments;
	}

	/**
	 * Returns the keyword starting at {@code index}, which must sit on whitespace.
	 */
	private LogicalOperator keywordAt(String condition, int index) {
		if (!Character.isWhitespace(condition.charAt(index))) {
			return null;
		}

		int keywordStart = index;
		while (keywordStart < condition.length() && Character.isWhitespace(condition.charAt(keywordStart))) {
			keywordStart++;
		}

		for (LogicalOperator candidate : LogicalOperator.values()) {
			String keyword = candidate.name().toLowerCase();
			int keywordEnd = keywordStart + keyword.length();

			boolean matches = condition.startsWith(keyword, keywordStart) && keywordEnd < condition.length()
					&& Character.isWhitespace(condition.charAt(keywordEnd));

			if (matches) {
				return candidate;
			}
		}

		return null;
	}

	private int skipKeyword(String condition, int index, LogicalOperator keyword) {
		int position = index;
		while (Character.isWhitespace(condition.charAt(position))) {
			position++;
		}

		position += keyword.name().length();

		while (position < condition.length() && Character.isWhitespace(condition.charAt(position))) {
			position++;
		}

		return position;
	}

	private LogicalOperator resolveLogicalOperator(List<Segment> segments) {
		LogicalOperator logicalOperator = null;

		for (Segment segment : segments) {
			if (segment.followingKeyword() == null) {
				continue;
			}

			if (logicalOperator != null && logicalOperator != segment.followingKeyword()) {
				throw invalidCondition("Routing condition must not mix 'and' and 'or'.");
			}

			logicalOperator = segment.followingKeyword();
		}

		return logicalOperator;
	}

	private Rule parseRule(String expression) {
		Matcher matcher = SIMPLE_CONDITION_PATTERN.matcher(expression);
		if (!matcher.matches()) {
			throw invalidCondition("Invalid routing condition rule: '" + expression + "'.");
		}

		Operator operator = Operator.fromExpression(matcher.group(2));
		String rawExpectedValue = matcher.group(3).trim();

		if (rawExpectedValue.isEmpty()) {
			throw invalidCondition("Routing condition rule '" + expression + "' must have an expected value.");
		}

		List<String> pathSegments = parseOutputPath(matcher.group(1).substring(OUTPUT_PREFIX.length()), expression);

		if (operator == Operator.IN) {
			return new Rule(pathSegments, operator, null, parseList(rawExpectedValue, expression));
		}

		if (rawExpectedValue.startsWith("[") || rawExpectedValue.endsWith("]")) {
			throw invalidCondition(
					"Routing condition rule '" + expression + "' uses a list with an unsupported operator.");
		}

		return new Rule(pathSegments, operator, parseScalar(rawExpectedValue, expression), List.of());
	}

	private List<String> parseOutputPath(String outputPath, String expression) {
		if (outputPath.isBlank()) {
			throw invalidCondition("Routing condition rule '" + expression + "' must reference an output field.");
		}

		List<String> pathSegments = List.of(PATH_SEPARATOR_PATTERN.split(outputPath, -1));
		for (String pathSegment : pathSegments) {
			if (pathSegment.isBlank()) {
				throw invalidCondition(
						"Routing condition rule '" + expression + "' contains an invalid output field path.");
			}
		}

		return pathSegments;
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

		List<Object> values = new ArrayList<>();
		for (String rawItem : splitListItems(listContent, expression)) {
			String item = rawItem.trim();
			if (item.isEmpty()) {
				throw invalidCondition("Routing condition rule '" + expression + "' contains an empty list value.");
			}

			values.add(parseScalar(item, expression));
		}

		return List.copyOf(values);
	}

	/**
	 * Commas inside quoted items are literal, so the split must respect quoting.
	 */
	private List<String> splitListItems(String listContent, String expression) {
		List<String> items = new ArrayList<>();
		int itemStart = 0;
		char openQuote = 0;

		for (int index = 0; index < listContent.length(); index++) {
			char character = listContent.charAt(index);

			if (openQuote != 0) {
				if (character == openQuote) {
					openQuote = 0;
				}
				continue;
			}

			if (character == '"' || character == '\'') {
				openQuote = character;
				continue;
			}

			if (character == ',') {
				items.add(listContent.substring(itemStart, index));
				itemStart = index + 1;
			}
		}

		if (openQuote != 0) {
			throw invalidCondition("Routing condition rule '" + expression + "' has an unterminated quoted value.");
		}

		items.add(listContent.substring(itemStart));
		return items;
	}

	private Object parseScalar(String rawValue, String expression) {
		String value = rawValue.trim();

		if (value.isEmpty()) {
			throw invalidCondition("Routing condition rule '" + expression + "' must have an expected value.");
		}

		if (isQuoted(value)) {
			return value.substring(1, value.length() - 1);
		}

		if (startsOrEndsWithQuote(value)) {
			throw invalidCondition("Routing condition rule '" + expression + "' contains an invalid quoted value.");
		}

		return switch (value) {
			case "true" -> Boolean.TRUE;
			case "false" -> Boolean.FALSE;
			case "null" -> null;
			default -> parseUnquotedValue(value, expression);
		};
	}

	private Object parseUnquotedValue(String value, String expression) {
		if (NUMERIC_PATTERN.matcher(value).matches()) {
			return new BigDecimal(value);
		}

		if (containsWhitespace(value)) {
			throw invalidCondition(
					"Routing condition rule '" + expression + "' must quote string values containing whitespace.");
		}

		return value;
	}

	private boolean isQuoted(String value) {
		return value.length() >= 2
				&& ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")));
	}

	private boolean startsOrEndsWithQuote(String value) {
		return value.startsWith("\"") || value.endsWith("\"") || value.startsWith("'") || value.endsWith("'");
	}

	private boolean containsWhitespace(String value) {
		for (int index = 0; index < value.length(); index++) {
			if (Character.isWhitespace(value.charAt(index))) {
				return true;
			}
		}

		return false;
	}

	private InvalidRoutingConditionException invalidCondition(String message) {
		return new InvalidRoutingConditionException(message);
	}

	private record Segment(String text, LogicalOperator followingKeyword) {
	}
}
