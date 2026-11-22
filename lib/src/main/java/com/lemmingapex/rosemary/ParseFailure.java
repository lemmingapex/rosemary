package com.lemmingapex.rosemary;

import java.io.Serial;

/**
 * Internal reason that a token or datetime could not be parsed.  {@link RosemaryDateTimeParser} reports it to callers as a {@link RosemaryDateTimeException}.
 */
final class ParseFailure extends Exception {
	@Serial
	private static final long serialVersionUID = 1L;

	ParseFailure(String message) {
		// failures are expected while parsing, so skip the stack trace
		super(message, null, false, false);
	}
}
