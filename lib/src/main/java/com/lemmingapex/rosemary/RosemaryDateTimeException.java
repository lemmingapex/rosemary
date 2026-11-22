package com.lemmingapex.rosemary;

import java.io.Serial;
import java.time.DateTimeException;

/**
 * Thrown when a datetime string can't be parsed.  It is unchecked, like the exceptions thrown by {@link java.time}.
 */
public class RosemaryDateTimeException extends DateTimeException {

	@Serial
	private static final long serialVersionUID = 2L;

	/**
	 * The string that could not be parsed.
	 */
	private final String parsedString;

	/**
	 * Create an exception for a string that could not be parsed.
	 *
	 * @param message the reason the string could not be parsed
	 * @param parsedString the string that could not be parsed
	 */
	public RosemaryDateTimeException(String message, String parsedString) {
		super(message);
		this.parsedString = parsedString;
	}

	/**
	 * Create an exception for a string that could not be parsed.
	 *
	 * @param message the reason the string could not be parsed
	 * @param parsedString the string that could not be parsed
	 * @param cause the underlying exception
	 */
	public RosemaryDateTimeException(String message, String parsedString, Throwable cause) {
		super(message, cause);
		this.parsedString = parsedString;
	}

	/**
	 * Get the string that could not be parsed.
	 *
	 * @return the string that could not be parsed
	 */
	public String getParsedString() {
		return parsedString;
	}
}
