package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Comparison rules for agent output values, which arrive as loosely typed JSON.
 */
final class RoutingConditionValues {

	private RoutingConditionValues() {
	}

	static boolean equal(Object actualValue, Object expectedValue) {
		if (actualValue == null || expectedValue == null) {
			return Objects.equals(actualValue, expectedValue);
		}

		if (actualValue instanceof Number actualNumber && expectedValue instanceof Number expectedNumber) {
			return toBigDecimal(actualNumber).compareTo(toBigDecimal(expectedNumber)) == 0;
		}

		if (!actualValue.getClass().equals(expectedValue.getClass())) {
			return false;
		}

		return actualValue.equals(expectedValue);
	}

	static boolean in(Object actualValue, List<Object> expectedValues) {
		return expectedValues.stream().anyMatch(expectedValue -> equal(actualValue, expectedValue));
	}

	static boolean contains(Object actualValue, Object expectedValue) {
		if (actualValue instanceof String actualString && expectedValue instanceof String expectedString) {
			return actualString.contains(expectedString);
		}

		if (actualValue instanceof Collection<?> actualCollection) {
			return actualCollection.stream().anyMatch(item -> equal(item, expectedValue));
		}

		return false;
	}

	private static BigDecimal toBigDecimal(Number value) {
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal;
		}

		return new BigDecimal(value.toString());
	}
}
