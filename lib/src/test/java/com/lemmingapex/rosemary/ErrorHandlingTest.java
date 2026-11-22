package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DateTimeException;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void isAnUncheckedDateTimeException() {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("not a date time"));
		assertInstanceOf(DateTimeException.class, e);
		assertInstanceOf(RuntimeException.class, e);
	}

	@Test
	void canBeCreatedByCallers() {
		final RuntimeException cause = new RuntimeException("cause");
		final RosemaryDateTimeException e = new RosemaryDateTimeException("message", "text", cause);
		assertEquals("message", e.getMessage());
		assertEquals("text", e.getParsedString());
		assertEquals(cause, e.getCause());
		assertNull(new RosemaryDateTimeException("message", "text").getCause());
	}

	@Test
	void returnsNullForNullText() {
		assertNull(PARSER.parse(null));
		assertNull(PARSER.parse(null, RosemaryDateOrder.DD_MM_YY));
	}
}
