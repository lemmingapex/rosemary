package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Dates without a time, which are midnight.  Today is 2024-06-15 in these tests.
 */
class DateParsingTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		03-05-2008                        | 2008-03-05T00:00Z
		3/5/21                            | 2021-03-05T00:00Z
		03-05-08                          | 2008-03-05T00:00Z
		003-005-02008                     | 2008-03-05T00:00Z
		2008-03-19                        | 2008-03-19T00:00Z
		2008/03/05                        | 2008-03-05T00:00Z
		5 March 2008                      | 2008-03-05T00:00Z
		March 5 2008                      | 2008-03-05T00:00Z
		March 5 08                        | 2008-03-05T00:00Z
		`March 5 '08`                     | 2008-03-05T00:00Z
		March 05 1988                     | 1988-03-05T00:00Z
		March 0005 002008                 | 2008-03-05T00:00Z
		`March, 5 2008`                   | 2008-03-05T00:00Z
		`March,, 5   2008`                | 2008-03-05T00:00Z
		`   March, , 5   2008    `        | 2008-03-05T00:00Z
		`March - 5 - 2008    `            | 2008-03-05T00:00Z
		`March -- 5 - 2008    `           | 2008-03-05T00:00Z
		`   March,  what?, 5   2008    `  | 2008-03-05T00:00Z
		Wed. March 5th 2008               | 2008-03-05T00:00Z
		Wednesday March 5th 2008          | 2008-03-05T00:00Z
		Sept. 5 2024                      | 2024-09-05T00:00Z
		Thurs. Sep 5 2024                 | 2024-09-05T00:00Z
		Dec 5 2024                        | 2024-12-05T00:00Z
		dec 5 2024                        | 2024-12-05T00:00Z
		0008-03-05                        | 0008-03-05T00:00Z
		0008-03-05T00:00Z                 | 0008-03-05T00:00Z
		""")
	void parsesDates(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0} {1}")
	@CsvSource(delimiter = '|', textBlock = """
		05.03.2008      | MM_DD_YY | 2008-05-03T00:00Z
		05.03.2008      | DD_MM_YY | 2008-03-05T00:00Z
		5.3.08          | DD_MM_YY | 2008-03-05T00:00Z
		2008.03.05      | DD_MM_YY | 2008-03-05T00:00Z
		05.03.2008 7:02 | DD_MM_YY | 2008-03-05T07:02Z
		""")
	void parsesDottedDates(String text, RosemaryDateOrder dateOrder, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text, dateOrder));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		March 5th 2008               | 2008-03-05T00:00Z
		22nd June 2028               | 2028-06-22T00:00Z
		22nd of June 2028            | 2028-06-22T00:00Z
		March Fifth 2008             | 2008-03-05T00:00Z
		March Thirty-first 1988      | 1988-03-31T00:00Z
		March twentythird 1988       | 1988-03-23T00:00Z
		The Fifth of March 2008      | 2008-03-05T00:00Z
		the first of march 2008      | 2008-03-01T00:00Z
		2008 March 1st               | 2008-03-01T00:00Z
		""")
	void parsesOrdinals(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	/**
	 * A month name or ordinal that comes after a number which was taken as the month or day moves that number.
	 */
	@ParameterizedTest(name = "{0} {1}")
	@CsvSource(delimiter = '|', textBlock = """
		3 March 2008     | MM_DD_YY | 2008-03-03T00:00Z
		3 March 5        | MM_YY_DD | 2003-03-05T00:00Z
		2008 3 March     | MM_DD_YY | 2008-03-03T00:00Z
		13 1st March     | MM_DD_YY | 2013-03-01T00:00Z
		5 13 1st         | YY_MM_DD | 2013-05-01T00:00Z
		5 1st 2008       | DD_MM_YY | 2008-05-01T00:00Z
		5 1st March      | DD_YY_MM | 2005-03-01T00:00Z
		5 2008 1st       | DD_MM_YY | 2008-05-01T00:00Z
		5 2008           | YY_MM_DD | 2008-05-01T00:00Z
		5 2008           | YY_DD_MM | 2008-06-05T00:00Z
		5 3 2008         | YY_MM_DD | 2008-03-05T00:00Z
		5 3 2008         | YY_DD_MM | 2008-05-03T00:00Z
		13 5 2008        | MM_DD_YY | 2008-05-13T00:00Z
		2008 13 5        | YY_MM_DD | 2008-05-13T00:00Z
		""")
	void movesValuesThatWereTakenTooEarly(String text, RosemaryDateOrder dateOrder, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text, dateOrder));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		March       | 2024-03-01T00:00Z
		March 5     | 2024-03-05T00:00Z
		6/19        | 2024-06-19T00:00Z
		5th         | 2024-06-05T00:00Z
		2008-03     | 2008-03-01T00:00Z
		Mar 2008    | 2008-03-01T00:00Z
		03-2008     | 2008-03-01T00:00Z
		1970        | 1970-01-01T00:00Z
		2008        | 2008-01-01T00:00Z
		2089        | 2089-01-01T00:00Z
		""")
	void fillsInPartialDates(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		junk 2024     | 2024-01-01T00:00Z
		Mayday 2024   | 2024-01-01T00:00Z
		hello 6/19    | 2024-06-19T00:00Z
		""")
	void ignoresOtherWords(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@Test
	void parsesLeapDays() {
		assertEquals(OffsetDateTime.parse("2024-02-29T00:00Z"), PARSER.parse("2/29/2024"));
		assertEquals(OffsetDateTime.parse("2000-02-29T00:00Z"), PARSER.parse("2/29/2000"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"2/29/2023", "2/29/1900", "2/30/2024", "2/31/2024", "4/31/2024 10:00", "13/13/2024", "0/5/2024", "March 32 2024", "0th March 2024", "2008 March 5th 6th", "2008 3 5 March", "March 5 2008 1st", "2008 13 14", "2008 13 1st", "40 2008", "13 14 2008", "2008 5 40"})
	void rejectsInvalidDates(String text) {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
	}
}
