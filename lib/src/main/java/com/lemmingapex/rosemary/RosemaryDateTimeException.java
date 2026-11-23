package com.lemmingapex.rosemary;

import java.io.Serial;
import java.time.format.DateTimeParseException;

/**
 * Thrown when a datetime string can't be parsed.  It is unchecked, and a {@link DateTimeParseException}, like the exceptions thrown
 * when {@link java.time} can't parse a string.
 * <p>
 * {@link #getParsedString()} is the string that couldn't be parsed.  {@link #getErrorIndex()} is the position of the part of the string
 * that couldn't be used, like a bad time or offset.  It is 0 when the problem is the string as a whole, like an impossible date
 * (February 30th), or no date or time at all.
 */
public class RosemaryDateTimeException extends DateTimeParseException {

	@Serial
	private static final long serialVersionUID = 3L;

	/**
	 * Create an exception for a string that could not be parsed.
	 *
	 * @param message the reason the string could not be parsed
	 * @param parsedString the string that could not be parsed
	 * @param errorIndex the position in the string of the problem, or 0 if the problem is the string as a whole
	 */
	public RosemaryDateTimeException(String message, CharSequence parsedString, int errorIndex) {
		super(message, parsedString, errorIndex);
	}

	/**
	 * Create an exception for a string that could not be parsed.
	 *
	 * @param message the reason the string could not be parsed
	 * @param parsedString the string that could not be parsed
	 * @param errorIndex the position in the string of the problem, or 0 if the problem is the string as a whole
	 * @param cause the underlying exception
	 */
	public RosemaryDateTimeException(String message, CharSequence parsedString, int errorIndex, Throwable cause) {
		super(message, parsedString, errorIndex, cause);
	}
}
