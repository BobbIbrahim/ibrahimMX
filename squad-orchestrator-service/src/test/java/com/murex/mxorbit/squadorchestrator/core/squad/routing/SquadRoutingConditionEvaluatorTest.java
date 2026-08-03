package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SquadRoutingConditionEvaluatorTest {

	private final SquadRoutingConditionEvaluator evaluator = new SquadRoutingConditionEvaluator();

	@Test
	void shouldEvaluateEqualsAsTrue() {
		assertTrue(evaluator.evaluate(output("changeType", "BUG_FIX"), "output.changeType equals BUG_FIX"));
	}

	@Test
	void shouldEvaluateEqualsAsFalse() {
		assertFalse(evaluator.evaluate(output("changeType", "BUG_FIX"), "output.changeType equals ENHANCEMENT"));
	}

	@Test
	void shouldEvaluateQuotedStringValue() {
		assertTrue(evaluator.evaluate(output("description", "urgent bug fix"),
				"output.description equals \"urgent bug fix\""));
	}

	@Test
	void shouldEvaluateSingleQuotedStringValue() {
		assertTrue(evaluator.evaluate(output("description", "urgent bug fix"),
				"output.description equals 'urgent bug fix'"));
	}

	@Test
	void shouldEvaluateNotEqualsAsTrue() {
		assertTrue(evaluator.evaluate(output("changeType", "BUG_FIX"), "output.changeType notEquals ENHANCEMENT"));
	}

	@Test
	void shouldEvaluateNotEqualsAsFalse() {
		assertFalse(evaluator.evaluate(output("changeType", "BUG_FIX"), "output.changeType notEquals BUG_FIX"));
	}

	@Test
	void shouldReturnFalseForNotEqualsWhenFieldIsMissing() {
		assertFalse(evaluator.evaluate(Map.of(), "output.changeType notEquals BUG_FIX"));
	}

	@Test
	void shouldEvaluateInAsTrue() {
		assertTrue(evaluator.evaluate(output("changeType", "BUG_FIX"), "output.changeType in [BUG_FIX, HOTFIX]"));
	}

	@Test
	void shouldEvaluateInAsFalse() {
		assertFalse(evaluator.evaluate(output("changeType", "ENHANCEMENT"), "output.changeType in [BUG_FIX, HOTFIX]"));
	}

	@Test
	void shouldEvaluateStringContainsAsTrue() {
		assertTrue(evaluator.evaluate(output("description", "urgent production bug"),
				"output.description contains urgent"));
	}

	@Test
	void shouldEvaluateStringContainsAsFalse() {
		assertFalse(
				evaluator.evaluate(output("description", "regular enhancement"), "output.description contains urgent"));
	}

	@Test
	void shouldEvaluateCollectionContainsAsTrue() {
		assertTrue(evaluator.evaluate(output("labels", List.of("backend", "urgent")), "output.labels contains urgent"));
	}

	@Test
	void shouldEvaluateCollectionContainsAsFalse() {
		assertFalse(
				evaluator.evaluate(output("labels", List.of("backend", "workflow")), "output.labels contains urgent"));
	}

	@Test
	void shouldEvaluateAndWhenAllRulesMatch() {
		Map<String, Object> sourceOutput = new LinkedHashMap<>();
		sourceOutput.put("changeType", "BUG_FIX");
		sourceOutput.put("test", "UNIT");

		assertTrue(evaluator.evaluate(sourceOutput, "output.changeType equals BUG_FIX and output.test equals UNIT"));
	}

	@Test
	void shouldEvaluateAndAsFalseWhenOneRuleDoesNotMatch() {
		Map<String, Object> sourceOutput = new LinkedHashMap<>();
		sourceOutput.put("changeType", "BUG_FIX");
		sourceOutput.put("test", "INTEGRATION");

		assertFalse(evaluator.evaluate(sourceOutput, "output.changeType equals BUG_FIX and output.test equals UNIT"));
	}

	@Test
	void shouldEvaluateOrAsTrueWhenOneRuleMatches() {
		assertTrue(evaluator.evaluate(output("changeType", "HOTFIX"),
				"output.changeType equals BUG_FIX or output.changeType equals HOTFIX"));
	}

	@Test
	void shouldEvaluateOrAsFalseWhenNoRuleMatches() {
		assertFalse(evaluator.evaluate(output("changeType", "ENHANCEMENT"),
				"output.changeType equals BUG_FIX or output.changeType equals HOTFIX"));
	}

	@Test
	void shouldEvaluateNestedOutputField() {
		Map<String, Object> classification = new LinkedHashMap<>();
		classification.put("changeType", "BUG_FIX");

		Map<String, Object> sourceOutput = new LinkedHashMap<>();
		sourceOutput.put("classification", classification);

		assertTrue(evaluator.evaluate(sourceOutput, "output.classification.changeType equals BUG_FIX"));
	}

	@Test
	void shouldReturnFalseWhenNestedOutputFieldIsMissing() {
		assertFalse(evaluator.evaluate(output("classification", Map.of()),
				"output.classification.changeType equals BUG_FIX"));
	}

	@Test
	void shouldReturnFalseWhenOutputMapIsNull() {
		assertFalse(evaluator.evaluate(null, "output.changeType equals BUG_FIX"));
	}

	@Test
	void shouldReturnFalseWhenOutputMapIsEmpty() {
		assertFalse(evaluator.evaluate(Map.of(), "output.changeType equals BUG_FIX"));
	}

	@Test
	void shouldReturnFalseWhenActualValueTypeDoesNotMatchExpectedValueType() {
		assertFalse(evaluator.evaluate(output("code", "1"), "output.code equals 1"));
	}

	@Test
	void shouldEvaluateIntegerAgainstNumericCondition() {
		assertTrue(evaluator.evaluate(output("count", 10), "output.count equals 10"));
	}

	@Test
	void shouldEvaluateLongAgainstNumericCondition() {
		assertTrue(evaluator.evaluate(output("count", 10L), "output.count equals 10"));
	}

	@Test
	void shouldEvaluateDecimalAgainstNumericCondition() {
		assertTrue(evaluator.evaluate(output("score", 10.5), "output.score equals 10.5"));
	}

	@Test
	void shouldEvaluateBooleanCondition() {
		assertTrue(evaluator.evaluate(output("approved", true), "output.approved equals true"));
	}

	@Test
	void shouldEvaluateNullConditionValue() {
		Map<String, Object> sourceOutput = new LinkedHashMap<>();
		sourceOutput.put("result", null);

		assertTrue(evaluator.evaluate(sourceOutput, "output.result equals null"));
	}

	@Test
	void shouldDistinguishNullValueFromMissingField() {
		assertFalse(evaluator.evaluate(Map.of(), "output.result equals null"));
	}

	@Test
	void shouldValidateSupportedSimpleCondition() {
		assertDoesNotThrow(() -> evaluator.validate("output.changeType equals BUG_FIX"));
	}

	@Test
	void shouldValidateSupportedAndCondition() {
		assertDoesNotThrow(() -> evaluator.validate("output.changeType equals BUG_FIX and output.test equals UNIT"));
	}

	@Test
	void shouldValidateSupportedOrCondition() {
		assertDoesNotThrow(
				() -> evaluator.validate("output.changeType equals BUG_FIX or output.changeType equals HOTFIX"));
	}

	@Test
	void shouldRejectNullCondition() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate(null));
	}

	@Test
	void shouldRejectBlankCondition() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("   "));
	}

	@Test
	void shouldRejectConditionWithoutOutputPrefix() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("changeType equals BUG_FIX"));
	}

	@Test
	void shouldRejectEmptyOutputField() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output. equals BUG_FIX"));
	}

	@Test
	void shouldRejectUnknownOperator() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output.changeType matches BUG_FIX"));
	}

	@Test
	void shouldRejectMissingExpectedValue() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output.changeType equals"));
	}

	@Test
	void shouldRejectMixedLogicalOperators() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate(
				"output.changeType equals BUG_FIX and output.test equals UNIT or output.status equals READY"));
	}

	@Test
	void shouldRejectParentheses() {
		assertThrows(IllegalArgumentException.class,
				() -> evaluator.validate("output.changeType equals BUG_FIX and (output.test equals UNIT)"));
	}

	@Test
	void shouldRejectMalformedInListWithoutBrackets() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output.changeType in BUG_FIX, HOTFIX"));
	}

	@Test
	void shouldRejectEmptyInList() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output.changeType in []"));
	}

	@Test
	void shouldRejectInListContainingEmptyItem() {
		assertThrows(IllegalArgumentException.class,
				() -> evaluator.validate("output.changeType in [BUG_FIX, , HOTFIX]"));
	}

	@Test
	void shouldRejectListWithEqualsOperator() {
		assertThrows(IllegalArgumentException.class,
				() -> evaluator.validate("output.changeType equals [BUG_FIX, HOTFIX]"));
	}

	@Test
	void shouldRejectUnquotedStringContainingWhitespace() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output.description equals urgent bug"));
	}

	@Test
	void shouldRejectUnclosedDoubleQuote() {
		assertThrows(IllegalArgumentException.class,
				() -> evaluator.validate("output.description equals \"urgent bug"));
	}

	@Test
	void shouldRejectUnclosedSingleQuote() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output.description equals 'urgent bug"));
	}

	@Test
	void shouldRejectEmptyRuleBeforeAnd() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate(" and output.changeType equals BUG_FIX"));
	}

	@Test
	void shouldRejectEmptyRuleAfterAnd() {
		assertThrows(IllegalArgumentException.class, () -> evaluator.validate("output.changeType equals BUG_FIX and "));
	}

	private static Map<String, Object> output(String key, Object value) {
		Map<String, Object> output = new LinkedHashMap<>();
		output.put(key, value);
		return output;
	}
}
