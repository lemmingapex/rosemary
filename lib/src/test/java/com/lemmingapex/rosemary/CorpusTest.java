package com.lemmingapex.rosemary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Datetimes as real programs, protocols and people write them, from <code>corpus.txt</code>.  The file explains its format.
 * To add a case, add a line to the file.
 */
class CorpusTest {

	private static final String CORPUS = "corpus.txt";

	/**
	 * A line of the corpus.
	 *
	 * @param line the line number, to find the case in the file
	 * @param text the string to parse
	 * @param expected the datetime it should be read as, or <code>null</code> if it should be rejected
	 * @param dateOrder the date order to parse with
	 */
	record Case(int line, String text, OffsetDateTime expected, RosemaryDateOrder dateOrder) {

		@Override
		public String toString() {
			return CORPUS + ":" + line + " " + text.replace("\n", "\\n") + (dateOrder == RosemaryDateOrder.MM_DD_YY ? "" : " (" + dateOrder + ")");
		}
	}

	static List<Case> corpus() {
		final List<Case> cases = new ArrayList<>();
		try (InputStream in = CorpusTest.class.getResourceAsStream(CORPUS)) {
			if (in == null) {
				throw new IllegalStateException(CORPUS + " not found");
			}
			final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			int number = 0;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				number++;
				if (!line.isBlank() && !line.startsWith("#")) {
					cases.add(parseLine(number, line));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return cases;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("corpus")
	void readsCorpus(Case c) {
		if (c.expected() == null) {
			assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(c.text(), c.dateOrder()));
		} else {
			assertEquals(c.expected(), PARSER.parse(c.text(), c.dateOrder()));
		}
	}

	private static Case parseLine(int number, String line) {
		final String[] columns = line.split("\\|");
		if (columns.length < 2 || columns.length > 3) {
			throw new IllegalArgumentException(CORPUS + ":" + number + ": expected 2 or 3 columns separated by |, but found " + columns.length);
		}
		final String expected = columns[1].trim();
		final RosemaryDateOrder dateOrder = columns.length == 3 ? RosemaryDateOrder.valueOf(columns[2].trim()) : RosemaryDateOrder.MM_DD_YY;
		return new Case(number, unescape(columns[0].trim()), expected.equals("error") ? null : OffsetDateTime.parse(expected), dateOrder);
	}

	/**
	 * Replace <code>\n</code>, <code>\t</code> and <code>\\</code> with a newline, a tab and a backslash.
	 */
	private static String unescape(String text) {
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (c == '\\' && i + 1 < text.length()) {
				final char next = text.charAt(++i);
				result.append(switch (next) {
					case 'n' -> '\n';
					case 't' -> '\t';
					case '\\' -> '\\';
					default -> throw new IllegalArgumentException("Unknown escape \\" + next + " in " + text);
				});
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}
}
