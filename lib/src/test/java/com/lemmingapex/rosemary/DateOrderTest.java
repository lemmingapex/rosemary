package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.OffsetDateTime;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The date order decides ambiguous dates, like 01/02/03.
 */
class DateOrderTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		MM_DD_YY | 2003-01-02T04:05Z
		MM_YY_DD | 2002-01-03T04:05Z
		DD_MM_YY | 2003-02-01T04:05Z
		DD_YY_MM | 2002-03-01T04:05Z
		YY_MM_DD | 2001-02-03T04:05Z
		YY_DD_MM | 2001-03-02T04:05Z
		""")
	void decidesAmbiguousDates(RosemaryDateOrder dateOrder, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse("01-02-03 04:05", dateOrder));
		assertEquals(expected, PARSER.toBuilder().dateOrder(dateOrder).build().parse("01-02-03 04:05"));
	}

	@Test
	void defaultsToMonthDayYear() {
		assertEquals(RosemaryDateOrder.MM_DD_YY, new RosemaryDateTimeParser().getDateOrder());
		assertEquals(OffsetDateTime.parse("2003-01-02T04:05Z"), PARSER.parse("01-02-03 04:05"));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		MM_DD_YY | 2024-06-07T00:00Z
		DD_MM_YY | 2024-07-06T00:00Z
		""")
	void decidesDatesWithoutYears(RosemaryDateOrder dateOrder, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse("6/7", dateOrder));
	}

	/**
	 * Values that can only be a day or a year are placed correctly, whatever the order.
	 */
	@ParameterizedTest
	@EnumSource(RosemaryDateOrder.class)
	void doesNotNeedTheOrderForUnambiguousDates(RosemaryDateOrder dateOrder) {
		assertEquals(OffsetDateTime.parse("2024-12-31T00:00Z"), PARSER.parse("2024-12-31", dateOrder));
		assertEquals(OffsetDateTime.parse("2024-12-31T00:00Z"), PARSER.parse("December 31 2024", dateOrder));
		assertEquals(OffsetDateTime.parse("2024-12-31T00:00Z"), PARSER.parse("31 Dec 2024", dateOrder));
	}

	/**
	 * A full year written first means year, month, day, like ISO 8601, unless the order puts the year first.
	 */
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		MM_DD_YY | 2024-12-05T00:00Z
		MM_YY_DD | 2024-12-05T00:00Z
		DD_MM_YY | 2024-12-05T00:00Z
		DD_YY_MM | 2024-12-05T00:00Z
		YY_MM_DD | 2024-12-05T00:00Z
		YY_DD_MM | 2024-05-12T00:00Z
		""")
	void readsAFullYearFirstAsYearMonthDay(RosemaryDateOrder dateOrder, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse("2024-12-05", dateOrder));
		assertEquals(expected, PARSER.parse("2024/12/05", dateOrder));
	}

	@ParameterizedTest(name = "{0} {1}")
	@CsvSource(delimiter = '|', textBlock = """
		31/12/2024 | MM_DD_YY | 2024-12-31T00:00Z
		12/31/2024 | DD_MM_YY | 2024-12-31T00:00Z
		""")
	void usesTheOnlyValidOrder(String text, RosemaryDateOrder dateOrder, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text, dateOrder));
	}
}
