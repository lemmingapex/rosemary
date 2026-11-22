package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Parsing to a {@link ZonedDateTime}, which keeps the time zone, and finding time zones by name.
 */
class ZonedParsingTest {

	/**
	 * The zone is the region when the string names one, the offset when it has one, and the default zone otherwise.
	 */
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		2019-03-05 07:02 America/Denver               | 2019-03-05T07:02-07:00[America/Denver]
		2019-03-05 07:02 america/denver               | 2019-03-05T07:02-07:00[America/Denver]
		2024-07-15 10:00 CST                          | 2024-07-15T10:00-05:00[America/Chicago]
		2024-01-15 10:00 CST                          | 2024-01-15T10:00-06:00[America/Chicago]
		2024-07-15 10:00 -04:00                       | 2024-07-15T10:00-04:00
		2024-07-15 10:00 -0400 (EST)                  | 2024-07-15T10:00-04:00
		2024-07-15 10:00 UTC                          | 2024-07-15T10:00Z
		2024-07-15 10:00                              | 2024-07-15T10:00Z
		2019-03-05 America/Denver                     | 2019-03-05T00:00-07:00[America/Denver]
		1709596800                                    | 2024-03-05T00:00Z
		2008-03-05T07:02:00+01:00[Europe/Paris]       | 2008-03-05T07:02+01:00[Europe/Paris]
		""")
	void keepsTheTimeZone(String text, ZonedDateTime expected) {
		assertEquals(expected, PARSER.parseZoned(text));
	}

	@Test
	void usesTheDefaultZone() {
		final RosemaryDateTimeParser denver = PARSER.toBuilder().defaultZone(ZoneId.of("America/Denver")).build();
		assertEquals(ZonedDateTime.parse("2024-07-15T10:00-06:00[America/Denver]"), denver.parseZoned("2024-07-15 10:00"));
		assertEquals(ZonedDateTime.parse("2024-07-15T10:00-06:00[America/Denver]"), denver.parseZoned("2024-07-15 10:00", "yyyy-MM-dd HH:mm"));
	}

	@Test
	void keepsTheTimeZoneWithPatterns() {
		assertEquals(ZonedDateTime.parse("2021-01-18T03:00-05:00[America/New_York]"), PARSER.parseZoned("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV"));
		assertEquals(ZonedDateTime.parse("2021-01-18T03:00-05:00"), PARSER.parseZoned("2021-01-18 03:00 -0500", List.of("yyyy-MM-dd", "yyyy-MM-dd HH:mm XX")));
	}

	@Test
	void usesTheDateOrder() {
		assertEquals(ZonedDateTime.parse("2003-02-01T00:00Z"), PARSER.parseZoned("01/02/03", RosemaryDateOrder.DD_MM_YY));
	}

	/**
	 * The reason to keep the region: date arithmetic follows its daylight saving rules.
	 */
	@Test
	void keepsDaylightSavingRulesForArithmetic() {
		// daylight saving time started in Denver on March 10th, 2024
		final String text = "2024-03-09 12:00 America/Denver";
		assertEquals(ZoneOffset.ofHours(-6), PARSER.parseZoned(text).plusDays(1).getOffset());
		assertEquals(ZoneOffset.ofHours(-7), PARSER.parse(text).plusDays(1).getOffset());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"March 5, 2008 7:02 pm", "2019-03-05 07:02 America/Denver", "2024-07-15 10:00 CST", "Tue, 15 Nov 1994 08:12:31 -0500 (EST)",
		"1204675245123", "7pm", "2008-03-05T07:02:00+01:00[Europe/Paris]"})
	void parseIsTheOffsetOfParseZoned(String text) {
		assertEquals(PARSER.parseZoned(text).toOffsetDateTime(), PARSER.parse(text));
		assertEquals(PARSER.parseZoned(text, RosemaryDateOrder.DD_MM_YY).toOffsetDateTime(), PARSER.parse(text, RosemaryDateOrder.DD_MM_YY));
	}

	@Test
	void rejectsTheSameStrings() {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parseZoned("February 30 2024"));
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parseZoned("2021-02-31", "yyyy-MM-dd"));
		assertThrows(IllegalArgumentException.class, () -> PARSER.parseZoned("2021-01-18", List.of()));
		assertThrows(NullPointerException.class, () -> PARSER.parseZoned("2021-01-18", (String) null));
		assertThrows(NullPointerException.class, () -> PARSER.parseZoned("2021-01-18", (RosemaryDateOrder) null));
	}

	@Test
	void returnsNullForNullText() {
		assertNull(PARSER.parseZoned(null));
		assertNull(PARSER.parseZoned(null, RosemaryDateOrder.DD_MM_YY));
		assertNull(PARSER.parseZoned(null, "yyyy-MM-dd"));
		assertNull(PARSER.parseZoned(null, List.of("yyyy-MM-dd")));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		PST                    | America/Los_Angeles
		pst                    | America/Los_Angeles
		CET                    | Europe/Paris
		America/Denver         | America/Denver
		america/indiana/indianapolis | America/Indiana/Indianapolis
		`  America/Denver  `   | America/Denver
		Europe/Amsterdam       | Europe/Amsterdam
		Etc/GMT+5              | Etc/GMT+5
		UTC                    | Z
		gmt                    | Z
		UT                     | Z
		z                      | Z
		Zulu                   | Z
		""")
	void findsTimeZones(String name, String expected) {
		assertEquals(Optional.of(ZoneId.of(expected)), PARSER.findTimeZone(name));
	}

	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"", " ", "MART", "NOVT", "America/Atlantis", "-05:00", "GMT+5", "Mar", "of"})
	void doesNotFindOtherNames(String name) {
		assertEquals(Optional.empty(), PARSER.findTimeZone(name));
	}

	@Test
	void findsTheParsersOwnAbbreviations() {
		final RosemaryDateTimeParser parser = PARSER.toBuilder()
			.timeZoneAbbreviations(Map.of("Eastern", ZoneId.of("America/New_York")))
			.timeZoneAbbreviation("SGT", ZoneId.of("Asia/Singapore"))
			.build();
		assertEquals(Optional.of(ZoneId.of("America/New_York")), parser.findTimeZone("eastern"));
		assertEquals(Optional.of(ZoneId.of("Asia/Singapore")), parser.findTimeZone("SGT"));
		assertEquals(Optional.empty(), parser.findTimeZone("PST"));
		// region ids and UTC are always found
		assertEquals(Optional.of(ZoneId.of("America/Los_Angeles")), parser.findTimeZone("America/Los_Angeles"));
		assertEquals(Optional.of(ZoneOffset.UTC), parser.findTimeZone("UTC"));
		assertThrows(NullPointerException.class, () -> parser.findTimeZone(null));
	}
}
