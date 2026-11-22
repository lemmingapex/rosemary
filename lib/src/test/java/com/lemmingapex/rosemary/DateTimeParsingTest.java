package com.lemmingapex.rosemary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Datetimes with both a date and a time, in common formats.
 */
class DateTimeParsingTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		2008-03-05T07:02:00                    | 2008-03-05T07:02:00Z
		2008-03-05'T'07:02:00                  | 2008-03-05T07:02:00Z
		2008-03-05 07:02:00                    | 2008-03-05T07:02:00Z
		2008-03-05 07:02                       | 2008-03-05T07:02:00Z
		2008-03-05 07:02:10.001                | 2008-03-05T07:02:10.001Z
		2008-03-05 07:02:10.019                | 2008-03-05T07:02:10.019Z
		2008-03-05 07:02:10.190                | 2008-03-05T07:02:10.190Z
		2008-03-05 07:02:10.19                 | 2008-03-05T07:02:10.190Z
		2008-03-05 07:02:10.2                  | 2008-03-05T07:02:10.200Z
		2008-03-05 7:02 am                     | 2008-03-05T07:02:00Z
		2008-03-05 7:02 AM                     | 2008-03-05T07:02:00Z
		2008-03-05 7:02 PM                     | 2008-03-05T19:02:00Z
		2008/03/05 7:02:10                     | 2008-03-05T07:02:10Z
		03/05 2008 7:02:10                     | 2008-03-05T07:02:10Z
		03-05 2008 7:02:10                     | 2008-03-05T07:02:10Z
		`March 5, 2008 7:02 am`                | 2008-03-05T07:02:00Z
		`Mar 5, 2008 7:02 am`                  | 2008-03-05T07:02:00Z
		`Mar 5, 2008 07:02`                    | 2008-03-05T07:02:00Z
		Wednesday March 5th 2008 7:02 am       | 2008-03-05T07:02:00Z
		Wednesday March Fifth 2008 7:02:13 pm  | 2008-03-05T19:02:13Z
		03/05/2008 07:02                       | 2008-03-05T07:02:00Z
		1/9/22 4:18                            | 2022-01-09T04:18:00Z
		03/05/08 07:02                         | 2008-03-05T07:02:00Z
		03/05/08 0:02                          | 2008-03-05T00:02:00Z
		03/05/08 21:02                         | 2008-03-05T21:02:00Z
		03-05-2008 7:02 pm                     | 2008-03-05T19:02:00Z
		`7/20/19 1:58:15'`                     | 2019-07-20T01:58:15Z
		7/1/2016 12:15:51 pm                   | 2016-07-01T12:15:51Z
		7/1/2016 12:15:51 am                   | 2016-07-01T00:15:51Z
		22nd of June 2028 at 8am               | 2028-06-22T08:00:00Z
		18:00 May 5 2024                       | 2024-05-05T18:00:00Z
		Fri Jul 30 00:10:03 UTC 2021           | 2021-07-30T00:10:03Z
		""")
	void parsesDateTimes(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}
}
