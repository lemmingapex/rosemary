package com.lemmingapex.rosemary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Times, with and without a date.  A time without a date is today, which is 2024-06-15 in these tests.
 */
class TimeParsingTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		07:02            | 2024-06-15T07:02:00Z
		7:02 pm          | 2024-06-15T19:02:00Z
		7pm              | 2024-06-15T19:00:00Z
		7 pm             | 2024-06-15T19:00:00Z
		pm 7:02          | 2024-06-15T19:02:00Z
		11:32:09.179     | 2024-06-15T11:32:09.179Z
		19:03:43         | 2024-06-15T19:03:43Z
		`17:22:21.452\n` | 2024-06-15T17:22:21.452Z
		0:00             | 2024-06-15T00:00:00Z
		23:59:59.999     | 2024-06-15T23:59:59.999Z
		7:02 p.m.        | 2024-06-15T19:02:00Z
		12:30 a.m.       | 2024-06-15T00:30:00Z
		7 P.M.           | 2024-06-15T19:00:00Z
		noon             | 2024-06-15T12:00:00Z
		midnight         | 2024-06-15T00:00:00Z
		""")
	void parsesTimesAsToday(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		3/5/2024 12pm          | 2024-03-05T12:00:00Z
		3/5/2024 12:30pm       | 2024-03-05T12:30:00Z
		3/5/2024 12:30 PM      | 2024-03-05T12:30:00Z
		3/5/2024 12am          | 2024-03-05T00:00:00Z
		3/5/2024 12:30am       | 2024-03-05T00:30:00Z
		3/5/2024 12:30 AM      | 2024-03-05T00:30:00Z
		3/5/2024 11:30am       | 2024-03-05T11:30:00Z
		3/5/2024 11:30pm       | 2024-03-05T23:30:00Z
		3/5/2024 1:00 am       | 2024-03-05T01:00:00Z
		3/5/2024 1:00 pm       | 2024-03-05T13:00:00Z
		3/5/2024 13:00 PM      | 2024-03-05T13:00:00Z
		""")
	void parsesAmAndPm(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		2024-03-05 07:02:10.1             | 2024-03-05T07:02:10.1Z
		2024-03-05 07:02:10.123456        | 2024-03-05T07:02:10.123456Z
		2024-03-05 07:02:10.123456789     | 2024-03-05T07:02:10.123456789Z
		2024-03-05T07:02:10.000000001Z    | 2024-03-05T07:02:10.000000001Z
		`2024-03-05 07:02:10,123`         | 2024-03-05T07:02:10.123Z
		`2024-03-05 07:02:10,5 PST`       | 2024-03-05T07:02:10.5-08:00
		""")
	void parsesFractionsOfASecond(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		`Mar 5, 2008 0702`        | 2008-03-05T07:02:00Z
		`Mar 5, 2008 1930`        | 2008-03-05T19:30:00Z
		March 5 2008 193005       | 2008-03-05T19:30:05Z
		March 5 2008 at 7         | 2008-03-05T07:00:00Z
		March 5 2008 at 7 pm      | 2008-03-05T19:00:00Z
		June 21 2022 3 PM MDT     | 2022-06-21T15:00:00-06:00
		`Friday the 13th of September 2024 at noon` | 2024-09-13T12:00:00Z
		March 5 2008 midnight PST | 2008-03-05T00:00:00-08:00
		""")
	void parsesTimesWithoutColonsAfterADate(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Mar 5, 2008 2460", "Mar 5, 2008 0760", "March 5 2008 99", "March 5 2008 12345", "2024-03-05 07:02:10.1234567890"})
	void rejectsInvalidTimesWithoutColons(String text) {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"24:00", "3/5/2024 25:00", "3/5/2024 10:60", "3/5/2024 10:30:60", "3/5/2024 13:00 AM", "3/5/2024 10:xx", "3/5/2024 1:2:3:4:5", "3/5/2024 10:30zm", "3/5/2024 10:00 noon"})
	void rejectsInvalidTimes(String text) {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		20080305T070210Z             | 2008-03-05T07:02:10Z
		2008-03-05T070210Z           | 2008-03-05T07:02:10Z
		20080305T0702Z               | 2008-03-05T07:02Z
		20080305T070210+0100         | 2008-03-05T07:02:10+01:00
		20080305T070210.123-05:00    | 2008-03-05T07:02:10.123-05:00
		2008-03-05 070210Z           | 2008-03-05T07:02:10Z
		""")
	void parsesIsoBasicTimesWithATimeZone(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		Mar 5 2008 7A.M.             | 2008-03-05T07:00Z
		Mar 5 2008 7P.M              | 2008-03-05T19:00Z
		Mar 5 2008 7:02p.m.          | 2008-03-05T19:02Z
		Mar 5 2008 13:00PM           | 2008-03-05T13:00Z
		Mar 5 7 PM                   | 2024-03-05T19:00Z
		7 pm Mar 5                   | 2024-03-05T19:00Z
		Tuesday 5 PM                 | 2024-06-15T17:00Z
		""")
	void parsesAmAndPmAttachedOrAfterADate(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"Mar 5 2008 13:00AM", "Mar 5 2008 13:00 AM", "2008-03-05 PM", "Mar 5 PM", "2008/03/05 AM", "PM 2008-03-05",
		"Mar 5 2008 7 W10", "Mar 5 2008 7:02 123Z", "2008-03-05 07:02 0702Z"})
	void rejectsAmOrPmThatCanNotBeUsedAndUnrecognizedTimes(String text) {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
	}
}
