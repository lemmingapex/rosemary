package com.lemmingapex.rosemary;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a datetime string into tokens, e.g. <code>"Mar 5, 2008 7:02 PM America/Denver"</code> into
 * <code>MAR</code>, <code>5</code>, <code>2008</code>, <code>7:02</code>, <code>PM</code> and <code>AMERICA/DENVER</code>.
 */
final class Tokenizer {

	private Tokenizer() {
	}

	/**
	 * Split an upper case datetime string into tokens.
	 * <ul>
	 * <li>whitespace, commas, parentheses and quotes separate tokens</li>
	 * <li>a slash separates tokens, unless it is between two letters, as in a region id like America/Denver</li>
	 * <li>a dash separates tokens, unless it follows whitespace, a letter or a parenthesis, where it is part of an offset or a name,
	 * as in -05:00, GMT-7 or Thirty-first, or it follows a time, as in 07:02:10-05:00</li>
	 * <li>a T between digits separates a date and a time, as in 2008-03-05T07:02</li>
	 * <li>a period between digits separates the parts of a date, as in 05.03.2008, but not the fraction of a second in a time</li>
	 * <li>a comma after the seconds of a time and before a digit starts the fraction of a second, as in 07:02:10,123</li>
	 * <li>tokens made only of dashes are dropped</li>
	 * </ul>
	 *
	 * @param text upper case datetime string
	 * @return the tokens
	 */
	static List<String> tokenize(String text) {
		final List<String> tokens = new ArrayList<>();
		final StringBuilder token = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			final char previous = i > 0 ? text.charAt(i - 1) : ' ';
			final char next = i + 1 < text.length() ? text.charAt(i + 1) : ' ';
			final boolean inTime = token.indexOf(":") >= 0;

			if (c == ',' && inTime && isDigit(next) && endsWithSeconds(token)) {
				token.append('.');
				continue;
			}

			final boolean separator = switch (c) {
				case ',', '(', ')', '\'', '"' -> true;
				case '/' -> !(Character.isLetter(previous) && Character.isLetter(next));
				case '-' -> !(isWhitespace(previous) || Character.isLetter(previous) || previous == '(' || previous == ')' || (inTime && isDigit(previous)));
				case 'T' -> isDigit(previous) && isDigit(next);
				case '.' -> isDigit(previous) && isDigit(next) && !inTime;
				default -> isWhitespace(c);
			};
			if (separator) {
				addToken(tokens, token);
			} else {
				token.append(c);
			}
		}
		addToken(tokens, token);
		return tokens;
	}

	/**
	 * Does the token end with the seconds of a time, like <code>07:02:10</code>?
	 */
	private static boolean endsWithSeconds(StringBuilder token) {
		final int length = token.length();
		return length >= 6 && token.charAt(length - 6) == ':' && isDigit(token.charAt(length - 5)) && isDigit(token.charAt(length - 4))
			&& token.charAt(length - 3) == ':' && isDigit(token.charAt(length - 2)) && isDigit(token.charAt(length - 1));
	}

	private static void addToken(List<String> tokens, StringBuilder token) {
		if (!token.chars().allMatch(c -> c == '-')) {
			tokens.add(token.toString());
		}
		token.setLength(0);
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isWhitespace(char c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c);
	}
}
