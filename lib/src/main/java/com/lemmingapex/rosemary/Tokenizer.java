package com.lemmingapex.rosemary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a datetime string into tokens, e.g. <code>"Mar 5, 2008 7:02 PM America/Denver"</code> into
 * <code>MAR</code>, <code>5</code>, <code>2008</code>, <code>7:02</code>, <code>PM</code> and <code>AMERICA/DENVER</code>.
 */
final class Tokenizer {

	/**
	 * A token, in upper case, and where it starts in the datetime string.
	 *
	 * @param text the token, in upper case
	 * @param start the position of the token's first character in the datetime string
	 */
	record Token(String text, int start) {
	}

	private Tokenizer() {
	}

	/**
	 * Split a datetime string into upper case tokens.
	 * <ul>
	 * <li>whitespace, commas, parentheses, quotes and apostrophes separate tokens</li>
	 * <li>a slash separates tokens, unless it is between two letters, as in a region id like America/Denver</li>
	 * <li>a dash separates tokens, as in 2008-03-05 or 05-Mar-2008, unless it is part of an offset or a name: after whitespace or a parenthesis,
	 * as in -05:00, after a word that isn't a month name, as in GMT-7 or Thirty-first, or after a time, as in 07:02:10-05:00</li>
	 * <li>a T between digits separates a date and a time, as in 2008-03-05T07:02, and the token after it is a time, as in 20080305T070210-05:00</li>
	 * <li>a period between digits, or between a digit and a letter, separates the parts of a date, as in 05.03.2008 or 05.Mar.2008,
	 * but not the fraction of a second in a time</li>
	 * <li>a comma after the seconds of a time and before a digit starts the fraction of a second, as in 07:02:10,123</li>
	 * <li>tokens made only of dashes are dropped</li>
	 * </ul>
	 * Tokens are upper-cased after splitting, so their positions are positions in the original string, even if upper-casing changes
	 * the length of some characters, like ß.
	 *
	 * @param text datetime string
	 * @param names month names, to tell a dash in a date, like 05-Mar-2008, from a dash in a name
	 * @return the tokens
	 */
	static List<Token> tokenize(String text, DateNames names) {
		final List<Token> tokens = new ArrayList<>();
		final StringBuilder token = new StringBuilder();
		int start = 0;
		// a token after a T between digits is a time, even without colons, like 070210 in 20080305T070210-05:00
		boolean afterT = false;
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			final char previous = i > 0 ? text.charAt(i - 1) : ' ';
			final char next = i + 1 < text.length() ? text.charAt(i + 1) : ' ';
			final boolean inTime = afterT || token.indexOf(":") >= 0;

			if (c == ',' && inTime && isDigit(next) && endsWithSeconds(token)) {
				token.append('.');
				continue;
			}

			final boolean separator = switch (c) {
				case ',', '(', ')', '\'', '"', '\u2018', '\u2019', '\u201C', '\u201D' -> true;
				case '/' -> !(Character.isLetter(previous) && Character.isLetter(next));
				case '-' -> isDashSeparator(token, previous, next, inTime, names);
				case 'T', 't' -> isDigit(previous) && isDigit(next);
				case '.' -> !inTime && ((isDigit(previous) && isDigit(next)) || (isDigit(previous) && Character.isLetter(next))
					|| (Character.isLetter(previous) && isDigit(next)));
				default -> isWhitespace(c);
			};
			if (separator) {
				addToken(tokens, token, start);
				start = i + 1;
				afterT = c == 'T' || c == 't';
			} else {
				token.append(c);
			}
		}
		addToken(tokens, token, start);
		return tokens;
	}

	/**
	 * Does a dash separate tokens?  A dash is part of a token when it is a sign, as in -05:00 or GMT-7, or joins the words of a name,
	 * as in Thirty-first or America/Port-au-Prince.  Otherwise it separates the parts of a date, as in 2008-03-05, 05-Mar-2008 or Mar-05-2008.
	 *
	 * @param token the token so far
	 * @param previous the character before the dash
	 * @param next the character after the dash
	 * @param inTime <code>true</code> if the token so far is a time, so a dash after it starts an offset, as in 07:02:10-05:00 or 7:02PM-05:00
	 * @param names month names
	 */
	private static boolean isDashSeparator(StringBuilder token, char previous, char next, boolean inTime, DateNames names) {
		if (isWhitespace(previous) || previous == '(' || previous == ')' || inTime) {
			return false;
		}
		if (Character.isLetter(previous)) {
			// a month name before a digit is part of a date, like Mar in 05-Mar-2008.  After other words, the dash is a sign, as in GMT-7 or Etc/GMT-5.
			return isDigit(next) && names.monthNumber(token.toString().toUpperCase(Locale.ROOT)) != null;
		}
		return true;
	}

	/**
	 * Does the token end with the seconds of a time, like <code>07:02:10</code>?
	 */
	private static boolean endsWithSeconds(StringBuilder token) {
		final int length = token.length();
		return length >= 6 && token.charAt(length - 6) == ':' && isDigit(token.charAt(length - 5)) && isDigit(token.charAt(length - 4))
			&& token.charAt(length - 3) == ':' && isDigit(token.charAt(length - 2)) && isDigit(token.charAt(length - 1));
	}

	private static void addToken(List<Token> tokens, StringBuilder token, int start) {
		if (!token.chars().allMatch(c -> c == '-')) {
			tokens.add(new Token(token.toString().toUpperCase(Locale.ROOT), start));
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
