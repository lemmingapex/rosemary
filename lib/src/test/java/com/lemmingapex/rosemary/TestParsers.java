package com.lemmingapex.rosemary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Parsers shared by the tests.  They use a fixed clock, so "today" is always {@link #TODAY}.
 */
final class TestParsers {
	/**
	 * The current time for the tests: Saturday, June 15th, 2024 at noon UTC.
	 */
	static final Instant NOW = Instant.parse("2024-06-15T12:00:00Z");
	static final String TODAY = "2024-06-15";
	static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	/**
	 * A parser with the default settings, except for the fixed clock.
	 */
	static final RosemaryDateTimeParser PARSER = RosemaryDateTimeParser.builder().clock(CLOCK).build();

	private TestParsers() {
	}
}
