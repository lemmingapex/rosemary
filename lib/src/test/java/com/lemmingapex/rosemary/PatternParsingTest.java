package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Parsing with {@link java.time.format.DateTimeFormatter} patterns.
 */
class PatternParsingTest {

	@ParameterizedTest(name = "{0} with {1}")
	@CsvSource(delimiter = '|', textBlock = """
		2021-01-18                            | yyyy-MM-dd                | 2021-01-18T00:00Z
		2021-01-18 3:00 PM                    | yyyy-MM-dd h:mm a         | 2021-01-18T15:00Z
		2021-01-18 3:00 PM America/New_York   | yyyy-MM-dd h:mm a VV      | 2021-01-18T15:00-05:00
		2021-06-18 3:00 PM America/New_York   | yyyy-MM-dd h:mm a VV      | 2021-06-18T15:00-04:00
		2021-06-18T19:00:00+0000              | yyyy-MM-dd'T'HH:mm:ssXX   | 2021-06-18T19:00Z
		20210618 19:00:00+0400                | yyyyMMdd HH:mm:ssXX       | 2021-06-18T19:00+04:00
		20210901 102611 America/Panama        | yyyyMMdd HHmmss VV        | 2021-09-01T10:26:11-05:00
		01-18-21 03:00 America/New_York       | MM-dd-yy HH:mm VV         | 2021-01-18T03:00-05:00
		18 January 2021                       | d MMMM uuuu               | 2021-01-18T00:00Z
		15:30                                 | HH:mm                     | 2024-06-15T15:30Z
		""")
	void parsesWithAPattern(String text, String pattern, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text, pattern));
	}

	@Test
	void usesTheFirstPatternThatMatches() {
		assertEquals(OffsetDateTime.parse("2025-01-03T00:00Z"), PARSER.parse("25-01-03", List.of("yyyy-MM HH:mm", "yy-MM-dd")));
		assertEquals(OffsetDateTime.parse("2023-05-25T00:00Z"), PARSER.parse("25-23-05", List.of("MM/dd", "dd-MM-yy h:mm", "dd-yy-MM")));
	}

	@Test
	void usesTheDefaultZoneWhenThePatternHasNone() {
		final RosemaryDateTimeParser denver = PARSER.toBuilder().defaultZone(ZoneId.of("America/Denver")).build();
		assertEquals(OffsetDateTime.parse("2021-01-18T15:00-07:00"), denver.parse("2021-01-18 15:00", "yyyy-MM-dd HH:mm"));
		assertEquals(OffsetDateTime.parse("2021-01-18T00:00-07:00"), denver.parse("2021-01-18", "yyyy-MM-dd"));
	}

	@Test
	void usesTheLocaleForNames() {
		final RosemaryDateTimeParser french = PARSER.toBuilder().locale(Locale.FRENCH).build();
		assertEquals(OffsetDateTime.parse("2024-03-05T00:00Z"), french.parse("05 mars 2024", "dd MMMM yyyy"));
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("05 mars 2024", "dd MMMM yyyy"));
	}

	@Test
	void rejectsTextThatDoesNotMatch() {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021/01/18", List.of("yyyy-MM-dd", "dd.MM.yyyy")));
		assertEquals("2021/01/18", e.getParsedString());
	}

	@Test
	void checksDatesStrictly() {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021-02-31", "yyyy-MM-dd"));
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021-02-29 10:00", "yyyy-MM-dd HH:mm"));
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021-04-31 10:00 America/Denver", "yyyy-MM-dd HH:mm VV"));
		assertEquals(OffsetDateTime.parse("2024-02-29T10:00Z"), PARSER.parse("2024-02-29 10:00", "yyyy-MM-dd HH:mm"));
	}

	@Test
	void rejectsBadPatterns() {
		assertThrows(IllegalArgumentException.class, () -> PARSER.parse("2021-01-18", "yyyy-MM-dd {"));
		assertThrows(IllegalArgumentException.class, () -> PARSER.parse("2021-01-18", List.of()));
		assertThrows(NullPointerException.class, () -> PARSER.parse("2021-01-18", (String) null));
		assertThrows(NullPointerException.class, () -> PARSER.parse("2021-01-18", (List<String>) null));
	}

	@Test
	void returnsNullForNullText() {
		assertNull(PARSER.parse(null, "yyyy-MM-dd"));
		assertNull(PARSER.parse(null, List.of("yyyy-MM-dd")));
	}
}
