package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RosemaryDateTimeParser#parseDetailed(String)}: what the string had, what was filled in, and whether the date was ambiguous.
 */
class ParsedDateTimeTest {

	/**
	 * The fields the string had: Y year, M month, D day, T time, Z time zone, and A if the date is ambiguous.
	 */
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		2024-03-05T10:00:00Z                      | 2024-03-05T10:00Z                       | YMDTZ
		2024-03-05 10:00                          | 2024-03-05T10:00Z                       | YMDT
		2024-03-05                                | 2024-03-05T00:00Z                       | YMD
		2024-12-05                                | 2024-12-05T00:00Z                       | YMD
		20241205                                  | 2024-12-05T00:00Z                       | YMD
		March 5 2024                              | 2024-03-05T00:00Z                       | YMD
		Tuesday March 5                           | 2024-03-05T00:00Z                       | MD
		12/12                                     | 2024-12-12T00:00Z                       | MD
		March 2024                                | 2024-03-01T00:00Z                       | YM
		2024                                      | 2024-01-01T00:00Z                       | Y
		March                                     | 2024-03-01T00:00Z                       | M
		5th                                       | 2024-06-05T00:00Z                       | D
		7pm                                       | 2024-06-15T19:00Z                       | T
		noon                                      | 2024-06-15T12:00Z                       | T
		10:00 America/Denver                      | 2024-06-15T10:00-06:00                  | TZ
		2019-03-05 America/Denver                 | 2019-03-05T00:00-07:00                  | YMDZ
		`Tue, 15 Nov 1994 08:12:31 -0500 (EST)`   | 1994-11-15T08:12:31-05:00               | YMDTZ
		1709596800                                | 2024-03-05T00:00Z                       | YMDTZ
		2008-03-05T07:02:00+01:00[Europe/Paris]   | 2008-03-05T07:02+01:00                  | YMDTZ
		01/02/03                                  | 2003-01-02T00:00Z                       | YMDA
		3/5/24                                    | 2024-03-05T00:00Z                       | YMDA
		3/5                                       | 2024-03-05T00:00Z                       | MDA
		3/5 7pm                                   | 2024-03-05T19:00Z                       | MDTA
		03052024                                  | 2024-03-05T00:00Z                       | YMDA
		24/03/05                                  | 2005-03-24T00:00Z                       | YMDA
		31/12/2024                                | 2024-12-31T00:00Z                       | YMD
		12/12/2024                                | 2024-12-12T00:00Z                       | YMD
		""")
	void saysWhatTheStringHad(String text, OffsetDateTime expected, String fields) {
		final RosemaryParsedDateTime parsed = PARSER.parseDetailed(text);
		assertEquals(expected, parsed.toOffsetDateTime());
		assertEquals(PARSER.parseZoned(text), parsed.toZonedDateTime());
		assertEquals(fields, (parsed.hasYear() ? "Y" : "") + (parsed.hasMonth() ? "M" : "") + (parsed.hasDay() ? "D" : "") + (parsed.hasTime() ? "T" : "")
			+ (parsed.hasZone() ? "Z" : "") + (parsed.isAmbiguous() ? "A" : ""));
	}

	/**
	 * The date order given decides ambiguous dates, and a date is ambiguous if month-first, day-first or year-first reads it differently.
	 */
	@ParameterizedTest(name = "{0} {1}")
	@CsvSource(delimiter = '|', textBlock = """
		01/02/03     | DD_MM_YY | 2003-02-01T00:00Z | true
		01/02/03     | YY_MM_DD | 2001-02-03T00:00Z | true
		01/02/03     | YY_DD_MM | 2001-03-02T00:00Z | true
		2024-12-05   | DD_MM_YY | 2024-12-05T00:00Z | false
		2024-12-05   | YY_MM_DD | 2024-12-05T00:00Z | false
		2024-12-05   | YY_DD_MM | 2024-05-12T00:00Z | true
		2024-12-05   | MM_YY_DD | 2024-12-05T00:00Z | false
		12/12/2024   | DD_MM_YY | 2024-12-12T00:00Z | false
		""")
	void usesTheDateOrder(String text, RosemaryDateOrder dateOrder, OffsetDateTime expected, boolean ambiguous) {
		final RosemaryParsedDateTime parsed = PARSER.parseDetailed(text, dateOrder);
		assertEquals(expected, parsed.toOffsetDateTime());
		assertEquals(ambiguous, parsed.isAmbiguous());
	}

	@Test
	void usesTheParsersDateOrder() {
		final RosemaryDateTimeParser dayFirst = PARSER.toBuilder().dateOrder(RosemaryDateOrder.DD_MM_YY).build();
		assertEquals(OffsetDateTime.parse("2024-05-03T00:00Z"), dayFirst.parseDetailed("3/5/24").toOffsetDateTime());
		assertTrue(dayFirst.parseDetailed("3/5/24").isAmbiguous());
	}

	@Test
	void keepsTheTimeZone() {
		final RosemaryParsedDateTime parsed = PARSER.parseDetailed("2019-03-05 07:02 America/Denver");
		assertEquals(ZonedDateTime.parse("2019-03-05T07:02-07:00[America/Denver]"), parsed.toZonedDateTime());
		assertTrue(parsed.hasZone());

		final RosemaryDateTimeParser denver = PARSER.toBuilder().defaultZone(ZoneId.of("America/Denver")).build();
		final RosemaryParsedDateTime defaultZone = denver.parseDetailed("2019-03-05 07:02");
		assertEquals(ZonedDateTime.parse("2019-03-05T07:02-07:00[America/Denver]"), defaultZone.toZonedDateTime());
		assertFalse(defaultZone.hasZone());
	}

	/**
	 * Each date order is read with the same date for today, even if the clock ticks.
	 */
	@Test
	void readsTheClockOnce() {
		final AtomicInteger reads = new AtomicInteger();
		final Clock counting = new Clock() {
			@Override
			public ZoneId getZone() {
				return ZoneOffset.UTC;
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return this;
			}

			@Override
			public Instant instant() {
				reads.incrementAndGet();
				return TestParsers.NOW;
			}
		};
		final RosemaryParsedDateTime parsed = PARSER.toBuilder().clock(counting).build().parseDetailed("3/5");
		assertEquals(OffsetDateTime.parse("2024-03-05T00:00Z"), parsed.toOffsetDateTime());
		assertTrue(parsed.isAmbiguous());
		assertEquals(1, reads.get());
	}

	@Test
	void rejectsWhatParseRejects() {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parseDetailed("February 30 2024"));
		assertEquals("Unable to parse \"February 30 2024\": Bad day 30 for month 2 of 2024", e.getMessage());
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parseDetailed("Wednesday"));
		assertNull(PARSER.parseDetailed(null));
		assertNull(PARSER.parseDetailed(null, RosemaryDateOrder.DD_MM_YY));
		assertThrows(NullPointerException.class, () -> PARSER.parseDetailed("2024", null));
	}

	@Test
	void isAValue() {
		final RosemaryParsedDateTime parsed = PARSER.parseDetailed("3/5 7pm");
		assertEquals(parsed, PARSER.parseDetailed("3/5 7pm"));
		assertEquals(parsed.hashCode(), PARSER.parseDetailed("3/5 7pm").hashCode());
		assertNotEquals(parsed, PARSER.parseDetailed("3/5 7pm UTC"));
		assertNotEquals(parsed, PARSER.parseDetailed("3/5/24 7pm"));
		assertNotEquals(parsed, PARSER.parseDetailed("3/5 7pm", RosemaryDateOrder.DD_MM_YY));
		assertNotEquals(parsed, "2024-03-05T19:00Z");
		assertEquals("RosemaryParsedDateTime[dateTime=2024-03-05T19:00Z, hasYear=false, hasMonth=true, hasDay=true, hasTime=true, hasZone=false, ambiguous=true]",
			parsed.toString());
	}
}
