package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DateTimeException;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Strings that can't be parsed.
 */
class ErrorHandlingTest {

	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"", "  ", "\t\n", "not a date time", ".:/-", "of the at", "---", "///", "Wednesday", "PST", "AM"})
	void rejectsStringsWithoutADatetime(String text) {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
	}

	@Test
	void reportsTheReasonAndTheString() {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2/31/2024"));
		assertEquals("2/31/2024", e.getParsedString());
		assertTrue(e.getMessage().startsWith("Unable to parse \"2/31/2024\": "), e.getMessage());
		assertTrue(e.getMessage().contains("Bad day 31"), e.getMessage());
	}

	@Test
	void isAnUncheckedDateTimeParseException() {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("not a date time"));
		assertInstanceOf(DateTimeParseException.class, e);
		assertInstanceOf(DateTimeException.class, e);
		assertInstanceOf(RuntimeException.class, e);
	}

	/**
	 * A problem with one part of the string is reported at that part, and the message says where.
	 */
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		2024-03-05 10:xx               | 11
		2024-03-05 10:00 +19:00        | 17
		March 32 2024                  | 9
		`   March 32 2024`             | 12
		2024-03-05 25:00               | 11
		2024-03-05 10:00 GMT+5:75      | 17
		Straße 5 2024 10:xx            | 14
		3/5/2024 13:00 AM              | 15
		99999999999 March              | 0
		March 5 2008 1st               | 13
		2024-03-05 10:00 -05:00 -05:00 | 24
		12                             | 0
		""")
	void reportsWhereTheProblemIs(String text, int index) {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
		assertEquals(index, e.getErrorIndex());
		assertTrue(e.getMessage().endsWith(" at index " + index), e.getMessage());
	}

	/**
	 * A problem with the string as a whole is reported at its start, and the message doesn't give a position.
	 */
	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"February 30 2024", "not a date time", "", "Wednesday"})
	void reportsProblemsWithTheWholeStringAtTheStart(String text) {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
		assertEquals(0, e.getErrorIndex());
		assertFalse(e.getMessage().contains(" at index "), e.getMessage());
	}

	@Test
	void reportsTheFurthestPositionForPatterns() {
		assertEquals(8, assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021-01-1x", "yyyy-MM-dd")).getErrorIndex());
		assertEquals(11, assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021-01-18 1x:00", List.of("yyyy/MM/dd", "yyyy-MM-dd HH:mm"))).getErrorIndex());
		// an invalid date is a problem with the whole string
		assertEquals(0, assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("2021-02-31", "yyyy-MM-dd")).getErrorIndex());
	}

	@Test
	void canBeCaughtAsADateTimeParseException() {
		try {
			PARSER.parse("2024-03-05 10:xx");
		} catch (DateTimeParseException e) {
			assertEquals("2024-03-05 10:xx", e.getParsedString());
			assertEquals(11, e.getErrorIndex());
			return;
		}
		throw new AssertionError("no exception");
	}

	@Test
	void canBeCreatedByCallers() {
		final RuntimeException cause = new RuntimeException("cause");
		final RosemaryDateTimeException e = new RosemaryDateTimeException("message", "text", 2, cause);
		assertEquals("message", e.getMessage());
		assertEquals("text", e.getParsedString());
		assertEquals(2, e.getErrorIndex());
		assertEquals(cause, e.getCause());
		assertNull(new RosemaryDateTimeException("message", "text", 0).getCause());
	}

	@Test
	void returnsNullForNullText() {
		assertNull(PARSER.parse(null));
		assertNull(PARSER.parse(null, RosemaryDateOrder.DD_MM_YY));
	}
}
