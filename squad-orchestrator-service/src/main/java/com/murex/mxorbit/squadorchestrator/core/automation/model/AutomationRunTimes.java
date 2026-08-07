package com.murex.mxorbit.squadorchestrator.core.automation.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * A recurring schedule has no meaningful execution date, so
 * {@link Automation#getRunTime()} is always represented on the fixed anchor
 * date 1970-01-01. Only the UTC hour/minute (and, for WEEKLY automations,
 * {@link Automation#getWeeklyDay()}) drive scheduling; the anchor date
 * itself carries no meaning and must never be interpreted as a real
 * execution date.
 */
public final class AutomationRunTimes {

	private static final LocalDate ANCHOR_DATE = LocalDate.of(1970, 1, 1);

	private AutomationRunTimes() {
	}

	/**
	 * Normalizes any incoming {@link OffsetDateTime} (whatever its date or
	 * offset) to the fixed anchor date at the equivalent UTC instant, so
	 * every layer consistently holds {@code 1970-01-01T<hour>:<minute>:<second>Z}.
	 */
	public static OffsetDateTime anchorToUtc(OffsetDateTime runTime) {
		if (runTime == null) {
			return null;
		}

		OffsetDateTime utc = runTime.withOffsetSameInstant(ZoneOffset.UTC);
		return utc.toLocalTime().atDate(ANCHOR_DATE).atOffset(ZoneOffset.UTC);
	}
}
