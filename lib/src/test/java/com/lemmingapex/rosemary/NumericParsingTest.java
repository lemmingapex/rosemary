package com.lemmingapex.rosemary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Numbers without separators.  A number by itself is read by its length.
 */
class NumericParsingTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		1969            | 1969-01-01T00:00Z
		1970            | 1970-01-01T00:00Z
		2024            | 2024-01-01T00:00Z
		0050            | 0050-01-01T00:00Z
		""")
	void readsFourDigitsAsAYear(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0} {1}")
	@CsvSource(delimiter = '|', textBlock = """
		03052008   | MM_DD_YY | 2008-03-05T00:00Z
		05032008   | DD_MM_YY | 2008-03-05T00:00Z
		20080305   | YY_MM_DD | 2008-03-05T00:00Z
		20080503   | YY_DD_MM | 2008-03-05T00:00Z
		99993112   | YY_DD_MM | 9999-12-31T00:00Z
		20240305   | MM_DD_YY | 2024-03-05T00:00Z
		20240305   | DD_MM_YY | 2024-03-05T00:00Z
		030508     | MM_DD_YY | 2008-03-05T00:00Z
		050308     | DD_MM_YY | 2008-03-05T00:00Z
		080305     | YY_MM_DD | 2008-03-05T00:00Z
		022924     | MM_DD_YY | 2024-02-29T00:00Z
		""")
	void readsSixAndEightDigitsAsADate(String text, RosemaryDateOrder dateOrder, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text, dateOrder));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		03052008 7:02 pm       | 2008-03-05T19:02Z
		20240305 10:00 PST     | 2024-03-05T10:00-08:00
		""")
	void readsCompactDatesWithTimes(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		100000000       | 1973-03-03T09:46:40Z
		1709596800      | 2024-03-05T00:00:00Z
		1204675245      | 2008-03-05T00:00:45Z
		99999999999     | 5138-11-16T09:46:39Z
		""")
	void readsNineToElevenDigitsAsEpochSeconds(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		100000000000    | 1973-03-03T09:46:40Z
		1204675200000   | 2008-03-05T00:00:00Z
		1204675245123   | 2008-03-05T00:00:45.123Z
		1709596800000   | 2024-03-05T00:00:00Z
		""")
	void readsTwelveAndThirteenDigitsAsEpochMilliseconds(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		20240305193005           | 2024-03-05T19:30:05Z
		20240305193005 PST       | 2024-03-05T19:30:05-08:00
		20240305193005 +0530     | 2024-03-05T19:30:05+05:30
		""")
	void readsFourteenDigitsAsADatetime(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"0", "5", "12", "123", "12345", "1234567", "99993113", "13132024", "20241332", "20240305256000", "123456789012345", "-5", "-1709596800",
		"99999999999 March", "March 99999999999999999999999", "March 5 2008 20240305193005", "March 12345"})
	void rejectsOtherNumbers(String text) {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
	}
}
