package com.lemmingapex.rosemary;

import java.io.Serial;

/**
 * Exception thrown when a date/time string cannot be parsed.
 */
public class RosemaryDateTimeException extends Exception {

	@Serial
	private static final long serialVersionUID = 7195725880623801298L;

	public RosemaryDateTimeException(String message) {
		super(message);
	}

	public RosemaryDateTimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public RosemaryDateTimeException(Throwable cause) {
		super(cause);
	}
}
