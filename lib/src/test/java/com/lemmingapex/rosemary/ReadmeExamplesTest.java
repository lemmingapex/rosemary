package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The examples in README.md.  Keep them in sync.
 */
class ReadmeExamplesTest {

	private static void assertParses(String expected, OffsetDateTime actual) {
		assertEquals(expected, actual.toString());
	}

	@Test
	void examples() {
		final RosemaryDateTimeParser rosemary = PARSER;
		assertParses("1988-03-05T00:00Z", rosemary.parse("March 05 1988"));
		assertParses("2024-03-05T00:00Z", rosemary.parse("3/5/24"));
		assertParses("2008-03-05T19:02:13Z", rosemary.parse("Wednesday March Fifth 2008 7:02:13 pm"));
		assertParses("2028-06-22T08:00Z", rosemary.parse("22nd of June 2028 at 8am"));
		assertParses("2019-03-05T07:02:30-07:00", rosemary.parse("2019-03-05 07:02:30 America/Denver"));
		assertParses("2008-03-05T07:02:10-05:00", rosemary.parse("03-05 2008 7:02:10 -05:00"));
		assertParses("2018-01-03T09:59-06:00", rosemary.parse("2018-01-03 09:59:00 CST"));
		assertParses("2016-06-30T10:02:27.654-04:00", rosemary.parse("6/30/2016 10:02:27.654 AM(UTC-4)"));
		assertParses("1994-11-15T08:12:31-05:00", rosemary.parse("Tue, 15 Nov 1994 08:12:31 -0500 (EST)"));
		assertParses("2024-03-05T10:00:00.123456789Z", rosemary.parse("2024-03-05T10:00:00.123456789Z"));
		assertParses("2008-03-05T00:00Z", rosemary.parse("05.03.2008", RosemaryDateOrder.DD_MM_YY));
		assertParses("2024-06-19T00:00Z", rosemary.parse("6/19"));
		assertParses("2024-06-15T19:00Z", rosemary.parse("7pm"));
		assertParses("1991-01-01T00:00Z", rosemary.parse("1991"));
		assertParses("2024-03-05T00:00Z", rosemary.parse("20240305"));
		assertParses("2024-03-05T00:00Z", rosemary.parse("1709596800"));
		assertParses("2008-03-05T00:00:45.123Z", rosemary.parse("1204675245123"));
		assertParses("2021-01-18T03:00-05:00", rosemary.parse("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV"));
	}

	@Test
	void ambiguousDates() {
		final RosemaryDateTimeParser rosemary = PARSER;
		assertParses("2003-01-02T00:00Z", rosemary.parse("01/02/03"));
		assertParses("2003-02-01T00:00Z", rosemary.parse("01/02/03", RosemaryDateOrder.DD_MM_YY));
		assertParses("2001-02-03T00:00Z", rosemary.parse("01/02/03", RosemaryDateOrder.YY_MM_DD));
		assertParses("2024-12-31T00:00Z", rosemary.parse("31/12/2024"));
		assertParses("2024-12-05T00:00Z", rosemary.parse("2024-12-05", RosemaryDateOrder.DD_MM_YY));
		assertParses("2024-05-12T00:00Z", rosemary.parse("2024-12-05", RosemaryDateOrder.YY_DD_MM));
	}

	@Test
	void builder() {
		final RosemaryDateTimeParser parser = RosemaryDateTimeParser.builder()
			.dateOrder(RosemaryDateOrder.DD_MM_YY)
			.defaultZone(ZoneId.of("Europe/London"))
			.clock(Clock.fixed(Instant.parse("2030-03-01T00:00:00Z"), ZoneOffset.UTC))
			.timeZoneAbbreviation("SGT", ZoneId.of("Asia/Singapore"))
			.build();
		assertParses("2030-03-05T19:02Z", parser.parse("5/3 7:02 pm"));
		assertParses("2030-07-05T19:02+01:00", parser.parse("5/7 7:02 pm"));
		assertParses("2030-03-05T19:02+08:00", parser.parse("5/3 7:02 pm SGT"));
	}

	@Test
	void whatTheStringHad() {
		final RosemaryParsedDateTime parsed = PARSER.parseDetailed("3/5 7pm");
		assertParses("2024-03-05T19:00Z", parsed.toOffsetDateTime());
		assertFalse(parsed.hasYear());
		assertTrue(parsed.hasMonth());
		assertTrue(parsed.hasDay());
		assertTrue(parsed.hasTime());
		assertFalse(parsed.hasZone());
		assertTrue(parsed.isAmbiguous());
		assertParses("2024-05-03T19:00Z", PARSER.parseDetailed("3/5 7pm", RosemaryDateOrder.DD_MM_YY).toOffsetDateTime());
		assertTrue(PARSER.parseDetailed("01/02/03").isAmbiguous());
		for (String text : List.of("31/12/2024", "March 5", "2024-12-05")) {
			assertFalse(PARSER.parseDetailed(text).isAmbiguous(), text);
		}
		assertFalse(PARSER.parseDetailed("March 5").hasYear());
	}

	@Test
	void languages() {
		final RosemaryDateTimeParser french = PARSER.toBuilder().locale(Locale.FRENCH).build();
		assertParses("2024-03-05T10:00Z", french.parse("mardi 5 mars 2024 10:00"));
		assertParses("2024-02-01T00:00Z", french.parse("1er févr. 2024"));
		assertParses("2024-03-05T10:00Z", french.parse("Tue, 05 Mar 2024 10:00:00 GMT"));
		assertParses("2024-03-05T00:00Z", PARSER.toBuilder().locale(Locale.forLanguageTag("ru")).build().parse("5 марта 2024"));
		assertParses("2024-02-05T00:00Z", french.parse("5 fevrier 2024"));
		assertThrows(RosemaryDateTimeException.class, () -> french.parse("5 jui 2024"));
	}

	@Test
	void patterns() {
		assertParses("2021-01-18T03:00-05:00", PARSER.parse("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV"));
		assertParses("2025-01-03T00:00Z", PARSER.parse("25-01-03", List.of("yyyy-MM HH:mm", "yy-MM-dd")));
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021-02-31", "yyyy-MM-dd"));
		assertParses("2024-03-05T00:00Z", PARSER.parse("03-05", "MM-dd"));
		assertParses("2024-03-01T00:00Z", PARSER.parse("2024-03", "yyyy-MM"));
	}

	@Test
	void timeZones() {
		final RosemaryDateTimeParser rosemary = PARSER;
		assertParses("2024-01-15T10:00-06:00", rosemary.parse("2024-01-15 10:00 CST"));
		assertParses("2024-07-15T10:00-05:00", rosemary.parse("2024-07-15 10:00 CST"));
		assertParses("2024-07-15T10:00Z", rosemary.parse("2024-07-15 10:00 GMT"));
		assertParses("2024-07-15T10:00-04:00", rosemary.parse("2024-07-15 10:00 -0400 (EST)"));
	}

	@Test
	void zonedDateTimes() {
		final RosemaryDateTimeParser rosemary = PARSER;
		assertEquals("2024-03-09T12:00-07:00[America/Denver]", rosemary.parseZoned("2024-03-09 12:00 America/Denver").toString());
		assertEquals("2024-03-10T12:00-06:00[America/Denver]", rosemary.parseZoned("2024-03-09 12:00 America/Denver").plusDays(1).toString());
		assertEquals("2024-07-15T10:00-05:00[America/Chicago]", rosemary.parseZoned("2024-07-15 10:00 CST").toString());
		assertEquals("2024-07-15T10:00-04:00", rosemary.parseZoned("2024-07-15 10:00 -04:00").toString());
	}

	@Test
	void findTimeZone() {
		final RosemaryDateTimeParser rosemary = PARSER;
		assertEquals("Optional[America/Los_Angeles]", rosemary.findTimeZone("pst").toString());
		assertEquals("Optional[America/Denver]", rosemary.findTimeZone("america/denver").toString());
		assertEquals("Optional[Z]", rosemary.findTimeZone("Zulu").toString());
		assertEquals("Optional.empty", rosemary.findTimeZone("MART").toString());
	}

	@Test
	void errors() {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("February 30 2024"));
		assertEquals("Unable to parse \"February 30 2024\": Bad day 30 for month 2 of 2024", e.getMessage());
		assertEquals("February 30 2024", e.getParsedString());
		assertEquals(0, e.getErrorIndex());

		final RosemaryDateTimeException time = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2024-03-05 10:xx"));
		assertEquals("Unable to parse \"2024-03-05 10:xx\": Bad time 10:XX at index 11", time.getMessage());
		assertEquals(11, time.getErrorIndex());
	}
}
