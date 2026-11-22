package com.lemmingapex.rosemary;

import java.io.Serial;

/**
 * Internal reason that a token or datetime could not be parsed.  {@link RosemaryDateTimeParser} reports it to callers as a {@link RosemaryDateTimeException}.
 */
final class ParseFailure extends Exception {
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Position in the string of the problem, or -1 if it isn't known yet, or the problem is the string as a whole.
	 */
	private int index = -1;

	ParseFailure(String message) {
		// failures are expected while parsing, so skip the stack trace
		super(message, null, false, false);
	}

	/**
	 * Set the position of the problem, unless it was already set.
	 *
	 * @param index position in the string
	 * @return this failure
	 */
	ParseFailure at(int index) {
		if (this.index < 0) {
			this.index = index;
		}
		return this;
	}

	/**
	 * @return the position in the string of the problem, or -1 if the problem is the string as a whole
	 */
	int index() {
		return index;
	}
}
